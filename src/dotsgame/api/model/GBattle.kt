package dotsgame.api.model

import dotsgame.api.EvUser
import dotsgame.entities.Room
import dotsgame.entities.User
import dotsgame.entities.newBattle
import dotsgame.enums.BattleOver
import dotsgame.server.Context
import zDb.find
import zDb.finder.equalsAny
import zDb.getDb
import zDb.transactionDb
import zUtils.DiList
import zUtils.badAlgorithm
import zUtils.badUnknownParam
import zUtils.myjson.JsonTransient
import java.time.LocalDateTime
import kotlin.math.roundToInt

class GBattle(
    val side0: Side,
    val side1: Side,
    val rules: GRules,
    var moveSide: Int = 0,
    var askDraw: Int? = null, // Сторона, предлагающая ничью
    var over: GOver? = null,
    val id: GBattleId = GBattles.newBattleId(),
) {
    var moveStartTime: Long = System.currentTimeMillis()

    @JsonTransient
    val room: GRoom = GRooms.forBattle(id)
    val roomId: GRoomId = room.id

    @JsonTransient
    val startTime: LocalDateTime = LocalDateTime.now()


    fun over(over: BattleOver, winSide: Int?) {
        if (this.over != null) {
            badUnknownParam("Партия уже окончена")
        }
        val gOver = getDb().transactionDb { db ->
            val users = db.find(
                User::id equalsAny DiList(side0.userId, side1.userId),
                User::title,
                User::level,
                User::score,
                User::battleCount,
                User::winCount,
                User::winByTimeoutCount,
                User::winByGroundCount,
                User::winLineCount,
                User::winWithStrongCount,
                User::winWithStrongLineCount,
                User::drawCount,
                User::looseCount,
                User::looseByTimeoutCount,
                User::looseByGroundCount,
                User::looseLineCount,
            ).getList()

            val sideUsers = DiList(
                users.first { it.id == side0.userId },
                users.first { it.id == side1.userId }
            )

            val scoreBefore: DiList<Int>
            scoreBefore = sideUsers.map { it.score }

            val eloSa = when (winSide) {
                null -> 0.5
                0 -> 1.0
                1 -> 0.0
                else -> badAlgorithm()
            }
            val eloEa = 1.0 / (1.0 + Math.pow(10.0, (scoreBefore[1] - scoreBefore[0]).toDouble() / 400.0))
            val eloK = sideUsers.sumByDouble { u ->
                when {
                    u.battleCount + 1 <= 30 -> 40.0
                    u.score < 2400 -> 20.0
                    else -> 10.0
                }
            } / 2.0

            val eloDa = (eloK * (eloSa - eloEa)).roundToInt()
            val scoreChange = DiList(eloDa, -eloDa)

            sideUsers[0].battleCount++
            sideUsers[1].battleCount++

            if (winSide != null) {
                sideUsers[winSide].winCount ++
                @Suppress("NON_EXHAUSTIVE_WHEN")
                when (over) {
                    BattleOver.FULL_FILL -> sideUsers[winSide].winByFillCount ++
                    BattleOver.TIMEOUT -> sideUsers[winSide].winByTimeoutCount ++
                    BattleOver.GROUND -> sideUsers[winSide].winByGroundCount ++
                }
                sideUsers[winSide].winLineCount ++
                sideUsers[winSide].looseLineCount = 0

                if (sideUsers[winSide].score < sideUsers[1 - winSide].score) {
                    sideUsers[winSide].winWithStrongCount ++
                    sideUsers[winSide].winWithStrongLineCount ++
                } else {
                    sideUsers[winSide].winWithStrongLineCount = 0
                }

                sideUsers[1 - winSide].looseCount ++
                @Suppress("NON_EXHAUSTIVE_WHEN")
                when (over) {
                    BattleOver.FULL_FILL -> sideUsers[1 - winSide].looseByFillCount ++
                    BattleOver.TIMEOUT -> sideUsers[1 - winSide].looseByTimeoutCount ++
                    BattleOver.GROUND -> sideUsers[1 - winSide].looseByGroundCount ++
                }
                sideUsers[winSide].winLineCount = 0
                sideUsers[winSide].looseLineCount ++
            } else {
                sideUsers[0].drawCount ++
                sideUsers[0].winLineCount = 0
                sideUsers[0].looseLineCount = 0
                sideUsers[0].winWithStrongLineCount = 0

                sideUsers[1].drawCount ++
                sideUsers[1].winLineCount = 0
                sideUsers[1].looseLineCount = 0
                sideUsers[1].winWithStrongLineCount = 0
            }
            sideUsers[0].score += scoreChange[0]
            sideUsers[1].score += scoreChange[1]
            db.save(sideUsers)
            synchronized(Context.connections) {
                for (connection in Context.connections) {
                    val userId = connection.userId
                    if (sideUsers[0].id == userId || sideUsers[1].id == userId) {
                        connection.updateUser()
                        Context.broadcastEvent(EvUser(connection.exUser))
                    }
                }
            }

            val room = Room()
            room.acquireId(roomId)
            val battle = newBattle(
                startTime = startTime,
                overTime = LocalDateTime.now(),
                room = room,
                over = over,
                winSide = winSide,
                moves = moves.toString(),
                sides = sideUsers,
                capturedPoints = DiList(side0.points, side1.points),
                scoreChange = scoreChange,
                scoreBefore = sideUsers.map { it.score },
                levels = sideUsers.map { it.level }
            )
            battle.acquireId(id)
            room.battle = battle
            db.save(room)
            db.save(battle)

            GOver(
                over,
                winSide,
                scoreBefore,
                scoreChange,
            )
        }
        this.over = gOver
        this.moveStartTime = System.currentTimeMillis()
        Context.broadcastEvent(EvBattlesAdd(listOf(header)))
        Context.broadcastEventToBattle(
            id,
            EvBattleOver(id, gOver)
        )

        when (over) {
            BattleOver.FULL_FILL -> if (winSide != null) {
                room.addSysMessage(side(winSide).userId, "побеждает — ходов больше нет.")
            } else {
                room.addSysMessage(null, "Ничья — ходов больше нет.")
            }
            BattleOver.SURRENDER -> if (winSide != null) {
                room.addSysMessage(side(winSide).userId, "побеждает — соперник сдался.")
            } else {
                room.addSysMessage(null, "Кто-то сдался.")
            }
            BattleOver.TIMEOUT -> if (winSide != null) {
                room.addSysMessage(side(winSide).userId, "побеждает — время вышло.")
            } else {
                room.addSysMessage(null, "Ничья — время вышло.")
            }
            BattleOver.DRAW -> if (winSide != null) {
                room.addSysMessage(side(winSide).userId, "побеждает — ничья (понимайте как хотите).")
            } else {
                room.addSysMessage(null, "Ничья.")
            }
            BattleOver.GROUND -> if (winSide != null) {
                room.addSysMessage(side(winSide).userId, "побеждает — заземлился.")
            } else {
                room.addSysMessage(null, "Ничья — кто-то заземлился (понимайте как хотите).")
            }
        }
    }

    fun side(): Side = side(moveSide)
    fun oppSide(): Side = side(1 - moveSide)

    fun side(side: Int): Side = when (side) {
        0 -> side0
        1 -> side1
        else -> badAlgorithm()
    }

    fun move(x: Int, y: Int) {
        val cap = field.addMove(x, y)
        val offset = moves.length
        moves.append(x.to62())
        moves.append(y.to62())
        if (cap > 0) {
            moves.append('!')
            moves.append(cap)
        }
        syncField()

        val now = System.currentTimeMillis()
        if (now > moveStartTime + rules.moveTime * 1000) {
            val minus = now - (moveStartTime + rules.moveTime * 1000)
            val oppSide = oppSide()
            oppSide.totalTime = maxOf(
                0,
                oppSide.totalTime - if (minus % 1000 == 0L) {
                    (minus / 1000).toInt()
                } else {
                    (minus / 1000 + 1).toInt()
                }
            )
        }
        moveStartTime = now

        val event = EvBattleMove(
            id,
            offset,
            moves.subSequence(offset, moves.length),
            this.moveSide,
            this.side0.points,
            this.side1.points,
            this.side0.totalTime,
            this.side1.totalTime,
            now
        )
        Context.broadcastEvent(event) { it.game.room?.id == roomId }

        checkWinByGround(this.moveSide)
        checkWinByGround(1 - this.moveSide)
    }

    @JsonTransient
    val moves = StringBuilder("ifjfjeie")
//    val moves = StringBuilder("tptqsqsptrrquq!1srrrtsrpurusvqvpuptnto!1soqluorlwqslvrtlttulssvl")
//    val moves = StringBuilder("0010C8ihjikhjhjgkglhlgmgmflfkflejeifmhnglineiemdkj!1of!1hfiggihggfghfhfifghigjhjhh!2gkfjhlei!1hohngngmhminfmgofl!1foencoeoepfpgpdpfq!1docqdqdrcrercpbqbpapaqbrao9pbn9o9n8n9m8mak9j8k9k9l8lbjbiaiaj9i8j7kbkcjcmdial!1dkcs!1dhclcn!1chcgdgbgefcejljkkkkl")
//    val moves = StringBuilder("0010C8ihjikhjhjgkglhlgmgmflfkflejeifmhnglineiemdkj!1of!1hfiggihggfghfhfifghigjhjhh!2gkfjhlei!1hohngngmhminfmgofl!1foencoeoepfpgpdpfq!1docqdqdrcrercpbqbpapaqbrao9pbn9o9n8n9m8mak9j8k9k9l8lbjbiaiaj9i8j7kbkcjal!1cibhchckdkcg7idjbgdh!1jlkmkllllmlkjmknjnjo!1onetesdsfsdudtcuctbubtauat9u9t7u8u8t7t6u8s!18v5s6t6s4t4q4s5r3q4o3n5m4n5n5o5p6o3o2o2p3p4p3r2n1o3m7n4l!1jsjririshshriqgshtitguhugteuftgvfufv!1kqkrlplqmompnnnoommnmmnm!1nlolpmnkmlojohnhnioiphqjqiqkrjrlslrmsnqnpnpornpi!1qpriqhsjrksk!1sfrgqgqfrfperhsh!1jcjdidkdjbichcibhbiahdhafbeaebdadbcacbbbbcabac9c9d8d9e8e8c9b7bb98f7f7e7d6e9f8g6c6d7c8b6g5c6b5a7a8a899a98a9a8!16f7g5g6h5j7j6l6k5k5l5i5h4h5f4g5d5e4e!17lg9faf9!1tptruqxpvpwquoulxiyhxnup!2wmvoyowozpvqyq11vcwcxcybwbxbwd!1vducv9w9x7d4c4a4d5b6e5g6f4h4e3g3d292c3!1m6l5p5p6q5q6r7r5v6q4u4p4p2o4o2o5!1oap9o8vnvmumunwsxqxswrvsvr!1tntmsoto!1rotktlujsm!1qorprqsrsq!1rsustsutstttvtss!2uruvuututvsvsu!1ooop!1npnq!1mqmr!1lrls!1ksktjtju!1oqpqorprpsosotns!1ouptqsqtqrovpupvquqvrurvmununtmt!1n4m5n5n3o3r3s2s3t3r2r1q2q1s6s7t6u7u6v5t7t8q8q7p8p7o7n7!1")

    @JsonTransient
    val header: GBattleHeader = GBattleHeader(this)

    @JsonTransient
    val field = run {
        val field = GBattleField(rules.width, rules.height)
        for (i in moves.indices step 2) {
            val mx = moves[i]
            if (mx == '!') {
                continue
            }
            val x = mx.from62()
            val y = moves[i + 1].from62()
            //logDebug("field.addMove({0}, {1})", x, y)
            field.addMove(x, y)
        }
        field
    }

    init {
        syncField()
    }

    private fun syncField() {
        moveSide = field.moveSide
        side0.points = field.score0
        side1.points = field.score1
    }

    private fun checkWinByGround(side: Int) {
        if (over == null) {
            val groundScore = field.groundScore(side)
            val win = when (side) {
                0 -> side0.points - groundScore.notGnd - groundScore.free > side1.points
                1 -> side1.points - groundScore.notGnd - groundScore.free > side0.points
                else -> badAlgorithm()
            }
            if (win) {
                over(BattleOver.GROUND, side)
            }
        }
    }

    class Side(
        val userId: GUserId,
        var points: Int,
        var totalTime: Int
    ) {
        constructor(user: GUserId, rules: GRules) : this(user, 0, rules.totalTime)

        @JsonTransient
        var lastAskDrawMove: Int? = null
    }


}
