<template>
    <div>
        <div :class="$style.Field__sides" style="padding-top: 20px">
<!--            <pre style="position: absolute"> {{ battle}}</pre>-->
            <template v-var="s = battle.side0, si = 0, u = getUser(s.userId)">
                <div :class="[$style.Field__side, battle.moveSide === si && $style.Field__moveSide]">
                    <User :user="u" :side="si" />
                    <div :class="[$style.Field__score, sideClass(si)]">{{s.points}}</div>
                    <div v-if="!battle.over" :class="$style.Field__totalTime">{{ formatMS(s.totalTime - (si === battle.moveSide ? moveTimeExtra : 0)) }}</div>
                    <div v-if="si === battle.moveSide && !battle.over" :class="$style.Field__moveTime">{{ formatMS(moveTime) }}</div>
                    <div v-if="battle.over && battle.over.winSide === si" :class="$style.Field__moveTime">Победитель</div>
                    <div v-else-if="battle.over && battle.over.winSide === null" :class="$style.Field__moveTime">Ничья</div>
                    <div v-if="battle.over" :class="$style.Field__scoreChange">{{ formatNumber(battle.over.scoreBefore[si]) + formatNumber(battle.over.scoreChange[si], true) }}</div>
                </div>
            </template>
            <template v-var="s = battle.side1, si = 1, u = getUser(s.userId)">
                <div :class="[$style.Field__side, battle.moveSide === si && $style.Field__moveSide]">
                    <User :user="u" :side="si" />
                    <div :class="[$style.Field__score, sideClass(si)]">{{s.points}}</div>
                    <div v-if="!battle.over" :class="$style.Field__totalTime">{{ formatMS(s.totalTime - (si === battle.moveSide ? moveTimeExtra : 0)) }}</div>
                    <div v-if="si === battle.moveSide && !battle.over" :class="$style.Field__moveTime">{{ formatMS(moveTime) }}</div>
                    <div v-if="battle.over && battle.over.winSide === si" :class="$style.Field__moveTime">Победитель</div>
                    <div v-else-if="battle.over && battle.over.winSide === null" :class="$style.Field__moveTime">Ничья</div>
                    <div v-if="battle.over" :class="$style.Field__scoreChange">{{ formatNumber(battle.over.scoreBefore[si]) + formatNumber(battle.over.scoreChange[si], true) }}</div>
                </div>
            </template>
        </div>
        <div v-if="mySide !== null" style="padding: 20px 40px 0 40px">
            <FButton :disabled="!!battle.over" @click="surrender">⚐ Сдаться</FButton>
            &nbsp;
            <FButton v-if="battle.askDraw === null" :disabled="!!battle.over" @click="draw(true)">⚖ Предложить ничью</FButton>
            <FButton v-else-if="battle.askDraw === mySide" :disabled="!!battle.over" @click="draw(false)">⚖ Отозвать предложение ничьи</FButton>
            <FButton v-else :disabled="!!battle.over" @click="draw(true)" :class="$style.Field_accDraw">⚖ Согласиться на ничью</FButton>

            <span v-if="errorHint" :class="$style.Field_errorHint">{{errorHint}}</span>
            <template v-else-if="battle.over">
                &nbsp;
                <template v-if="battle.over.over === 'SURRENDER' && battle.over.winSide === mySide">Соперник сдался — игра окончена</template>
                <template v-else-if="battle.over.over === 'SURRENDER'">Вы сдались — игра окончена</template>
                <template v-else-if="battle.over.over === 'GROUND' && battle.over.winSide === mySide">Вы заземлились — игра окончена</template>
                <template v-else-if="battle.over.over === 'GROUND'">Соперник заземлилися — игра окончена</template>
                <template v-else-if="battle.over.over === 'DRAW'">Ничья — игра окончена</template>
                <template v-else-if="battle.over.over === 'TIMEOUT'">Время вышло — игра окончена</template>
                <template v-else-if="battle.over.over === 'FULL_FILL'">Ходов больше нет — игра окончена</template>
                <template v-else>{{ battle.over.over }} — игра окончена</template>
            </template>
            <div style="float: right">
                <FNumber :min="1" :max="mover.moveCount" v-model="moveI" size="2" :class="moveI < mover.moveCount && $style.Field_move_warn"/>
            </div>
        </div>
        <!--  39 32 -->
        <svg :viewBox="`0.5 0.5 ${(cols - 1) * cellSize + paddings * 2}.5 ${(rows - 1) * cellSize + paddings * 2}.5`" :width="(cols - 1) * cellSize + paddings * 2 " :height="(rows - 1) * cellSize + paddings * 2" style="margin:0;vertical-align:top">
            <path :d="backgroundPath" :class="$style.backgroundPath"/>
            <g :class="[$style.labels, $style.topLabels]"><text v-for="x of cols" :x="paddings + (x - 1) * cellSize" :y="paddings - circleRadius - textGap">{{ x }}</text></g>
            <g :class="[$style.labels, $style.bottomLabels]"><text v-for="x of cols" :x="paddings + (x - 1) * cellSize" :y="paddings + (rows - 1) * cellSize + circleRadius + textGap">{{ x }}</text></g>
            <g :class="[$style.labels, $style.leftLabels]"><text v-for="y of rows" :x="paddings - circleRadius - textGap" :y="paddings + (rows - 1) * cellSize - (y - 1) * cellSize">{{ y }}</text></g>
            <g :class="[$style.labels, $style.rightLabels]"><text v-for="y of rows" :x="paddings + (cols - 1) * cellSize + circleRadius + textGap" :y="paddings + (rows - 1) * cellSize - (y - 1) * cellSize">{{ y }}</text></g>
            <g :class="$style.field" :transform="`translate(${paddings - cellSize}.5, ${paddings - cellSize}.5)`">
                <g :class="[$style.circles, sideClass(battle.moveSide)]" @click.prevent="circleClick">
                    <template v-for="x of cols">
                        <template v-for="y of rows" v-var="d = dot(x - 1, y - 1), dLast = d.move === moveI - 1 ? {x, y} : dLast">
                            <circle v-if="d.side !== -1 || d.captured" :r="circleRadius" :cx="x * cellSize -.5" :cy="(rows - y + 1) * cellSize-.5" :class="[sideClass(d.move < moveI ? d.side : undefined), d.gnd && /*d.side !== -1 && !d.captured && */$style.Field_gnd, d._notGnd && $style.Field_notGnd, d._free && $style.Field_free]"/>
                            <a v-else :href="'#' + x + ':' + y"><circle :r="circleRadius" :cx="x * cellSize -.5" :cy="(rows - y + 1) * cellSize-.5" :class="[sideClass(d.move < moveI ? d.side : undefined), d.gnd && /*d.side !== -1 && !d.captured && */$style.Field_gnd, d._notGnd && $style.Field_notGnd, d._free && $style.Field_free]"/></a>
                        </template>
                    </template>
                </g>
                <g :class="$style.paths">
                    <template v-for="p of mover.paths">
                        <path v-if="p.move < moveI" :d="p.d" :class="sideClass(p.side)"/>
                    </template>
                </g>
                <circle v-if="dLast" :r="1" :cx="dLast.x * cellSize -.5" :cy="(rows - dLast.y + 1) * cellSize-.5" :class="$style.last"/>
            </g>
        </svg>
    </div>
</template>

<script>
import User from "../User";
import { YES_NO_BUTTON } from "../../common/Desktop/MessageWin";
import {Mover} from "../Mover";
import {battleDraw, battleMove, battleSurrender} from "api";
import {getUser} from "../Users";
import {formatMS, formatNumber} from "../../common/misc";
import FButton from "../../common/Forms/FButton";
import moveSound from "../../sound/move.ogg";
import capSound from "../../sound/cap.ogg";
import {errorToString} from "../../common/api0";
import {nextTick} from "vue";
import FText from "../../common/Forms/FText";
import FNumber from "../../common/Forms/FNumber";

function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms))
}

function to62(num) {
    if (num >= 0 && num <= 9) {
        return String.fromCharCode(0x30/*0*/ + num)
    } else if (num >= 10 && num <= 35) {
        return String.fromCharCode(0x61/*a*/ + num - 10)
    } else if (num >= 36 && num <= 61) {
        return String.fromCharCode(0x41/*A*/ + num - 36)
    } else {
        throw Error('Overflow')
    }
}
function from62(char) {
    let code = char.charCodeAt(0)
    if (code >= 0x30 && code <= 0x39) {
        return code - 0x30
    } else if (code >= 0x61 && code <= 0x7a) {
        return code - 87
    } else if (code >= 0x41 && code <= 0x5a) {
        return code - 29
    } else {
        throw Error(`Overflow «${char}»`)
    }
}

export default {
    components: {FNumber, FText, FButton, User},
    props: {
        circleRadius: {default: 5},
        cellSize: {default: 20},
        paddings: {default: 40},
        textGap: {default: 5},

        battle: {
            type: Object,
            required: true
        },
    },
    beforeCreate() {
        this.soundMove = new Audio(moveSound)
        this.soundCap = new Audio(capSound)
    },
    beforeMount() {
        if (!this.battle.over) {
            this.startTimer()
        }
    },
    beforeUnmount() {
        clearTimeout(this.nowTimer)
    },
    data() {
        // let moves = '0010C8ihjikhjhjgkglhlgmgmflfkflejeifmhnglineiemdkj!1of!1hfiggihggfghfhfifghigjhjhh!2gkfjhlei!1hohngngmhminfmgofl!1foencoeoepfpgpdpfq!1docqdqdrcrercpbqbpapaqbrao9pbn9o9n8n9m8mak9j8k9k9l8lbjbiaiaj9i8j7kbkcjal!1cibhchckdkcg7idjbgdh!1jlkmkllllmlkjmknjnjo!1onetesdsfsdudtcuctbubtauat9u9t7u8u8t7t6u8s!18v5s6t6s4t4q4s5r3q4o3n5m4n5n5o5p6o3o2o2p3p4p3r2n1o3m7n4l!1jsjririshshriqgshtitguhugteuftgvfufv!1kqkrlplqmompnnnoommnmmnm!1nlolpmnkmlojohnhnioiphqjqiqkrjrlslrmsnqnpnpornpi!1qpriqhsjrksk!1sfrgqgqfrfperhsh!1jcjdidkdjbichcibhbiahdhafbeaebdadbcacbbbbcabac9c9d8d9e8e8c9b7bb98f7f7e7d6e9f8g6c6d7c8b6g5c6b5a7a8a899a98a9a8!16f7g5g6h5j7j6l6k5k5l5i5h4h5f4g5d5e4e!17lg9faf9!1tptruqxpvpwquoulxiyhxnup!2wmvoyowozpvqyq11vcwcxcybwbxbwd!1vducv9w9x7d4c4a4d5b6e5g6f4h4e3g3d292c3!1m6l5p5p6q5q6r7r5v6q4u4p4p2o4o2o5!1oap9o8vnvmumunwsxqxswrvsvr!1tntmsoto!1rotktlujsm!1qorprqsrsq!1rsustsutstttvtss!2uruvuututvsvsu!1ooop!1npnq!1mqmr!1lrls!1ksktjtju!1oqpqorprpsosotns!1ouptqsqtqrovpupvquqvrurvmununtmt!1n4m5n5n3o3r3s2s3t3r2r1q2q1s6s7t6u7u6v5t7t8q8q7p8p7o7n7!1'
        let mover = new Mover(this.battle.rules.width, this.battle.rules.height, this.cellSize)
        mover.follow(this.battle.moves)
        return {
            mover,
            processed: this.battle.moves.length,
            moveTime: null,
            moveTimeExtra: null,
            errorHint: null,
            moveI: mover.moveCount
        }
    },
    watch: {
        "battle.over": {
            immediate: false,
            handler(v) {
                if (v) {
                    clearTimeout(this.nowTimer)
                    this.soundCap.play()
                }
            }
        },
        "battle.moves": {
            immediate: false,
            handler(v) {
                let moves = v.substr(this.processed)
                if (moves) {
                    let moveCount = this.mover.moveCount
                    this.mover.follow(moves)
                    if (moveCount <= this.moveI) {
                        this.moveI = this.mover.moveCount
                    }
                    this.processed = v.length
                    this.soundMove.currentTime = 0
                    clearTimeout(this.nowTimer)
                    this.startTimer()
                    if (moves.indexOf('!') >=0) {
                        this.soundCap.play()
                    } else {
                        this.soundMove.play()
                    }
                }
            }
        }
    },
    methods: {
        getUser,
        formatMS,
        formatNumber,
        startTimer() {
            let battle = this.battle
            let elapsed = Date.now() - battle.moveStartTimeLocal
            if (elapsed > battle.rules.moveTime * 1000) {
                this.moveTime = 0
                this.moveTimeExtra = Math.ceil((elapsed - battle.rules.moveTime * 1000) / 1000)
            } else {
                this.moveTime = Math.floor((battle.rules.moveTime * 1000 - elapsed) / 1000)
                this.moveTimeExtra = 0
            }
            this.nowTimer = setTimeout(
                this.startTimer,
                1000 - Date.now() % 1000
            )
        },
        dot(x, y) {
            return this.mover.dots[x * this.mover.rows + y]
        },
        circleClick(e) {
            let a
            if (e.target.tagName === 'circle') {
                a = e.target.parentNode
                if (a.tagName !== 'a') {
                    return;
                }
            } else if (e.target.tagName === 'a') {
                a = e.target
            } else {
                return
            }
            let coo = a.getAttribute('href').substr(1).split(':')
            this.dotClick(parseInt(coo[0]) - 1, parseInt(coo[1]) - 1)
        },
        async dotClick(x, y) {
            try {
                if (this.mySide == null) {
                    this.hintError("Вы — зритель")
                } else if (this.battle.over) {
                    this.hintError("Игра окончена")
                } else if (this.mySide !== this.battle.moveSide) {
                    this.hintError("Не ваш ход")
                } else {
                    await battleMove({
                        battleId: this.battle.id,
                        x,
                        y
                    })
                }
            } catch (e) {
                this.hintError(errorToString(e))
            }
        },
        async surrender() {
            let r = await this.$msg({
                message: 'Уверены, что хотите сдаться?',
                buttons: YES_NO_BUTTON
            })
            if (r.result === 'yes') {
                try {
                    await battleSurrender({battleId: this.battle.id})
                } catch (e) {
                    this.hintError(errorToString(e))
                }
            }
        },
        async draw(ask) {
            try {
                await battleDraw({battleId: this.battle.id, ask})
            } catch (e) {
                this.hintError(errorToString(e))
            }
        },
        async hintError(errStr) {
            if (this.errorHintTime) {
                clearTimeout(this.errorHintTime)
            }
            if (this.errorHint) {
                this.errorHint = null
                await nextTick()
            }
            this.errorHint = errStr
            this.errorHintTime = setTimeout(() => {
                this.errorHint = null
            }, 2000)
        }
    },
    computed: {
        mySide() {
            let userId = this.$root.user && this.$root.user.id
            if (!userId) {
                return null
            }
            if (this.battle.side0.userId === userId) {
                return 0
            }
            if (this.battle.side1.userId === userId) {
                return 1
            }
            return null
        },
        cols() {
            return this.battle.rules.width
        },
        rows() {
            return this.battle.rules.height
        },
        width() {
            return (this.cols - 1) * 20
        },
        height() {
            return (this.rows - 1) * 20
        },
        backgroundPath() {
            let result = []
            let width = (this.cols - 1) * this.cellSize
            let height = (this.rows - 1) * this.cellSize
            for(let x=0; x<this.cols; x++) {
                result.push("M")
                result.push(x * this.cellSize + this.paddings)
                result.push(this.paddings)
                result.push("l")
                result.push(0)
                result.push(height)
            }
            for(let y=0; y<this.rows; y++) {
                result.push("M")
                result.push(this.paddings)
                result.push(y * this.cellSize + this.paddings)
                result.push("l")
                result.push(width)
                result.push(0)
            }
            return result.join(' ')
        }
    }
}
</script>

<style lang="scss" module>
.Field__sides {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  flex-wrap: nowrap;
  width: 100%;
  box-sizing: border-box;
  padding: 0 40px;
}
.Field__side {
    display: flex;
    flex-direction: row;
}
.Field__moveSide {

}
.Field__moveTime {
    border-radius: 1em 1em;
    //border: 1px solid red;
    padding: 0 .3em;
    font-weight: bold;
    color: white;
    padding-top: .1em;
    margin-top: -.1em;
}
.Field__side:nth-child(1) {
    > *:not(:first-child) {
        margin-left: 1em;
    }
}
.Field__side:nth-child(2) {
    flex-direction: row-reverse;
    > *:not(:first-child) {
        margin-right: 1em;
    }
}

@each $side in $sides {
    .Field__score.side#{map.get($side, index)} {
        color: map.get($side, color);
    }
    .Field__side:nth-child(#{map.get($side, index) + 1}) .Field__moveTime {
        background-color: map.get($side, color);
    }
}


.Field__score {
  font-weight: bold;
}

.backgroundPath {
    stroke: silver;
    stroke-width: 1px;
}

.circles {
    @each $side in $sides {
        &.side#{map.get($side, index)} circle.sideZ:hover {
            fill: map.get($side, color);
            fill-opacity: .5;
        }
    }
    //&.blue circle:not(.red):not(.blue):hover {
    //    fill: blue;
    //    fill-opacity: .5;
    //}
    //&.red circle:not(.red):not(.blue):hover {
    //    fill: red;
    //    fill-opacity: .5;
    //}
}
.circles circle {
    fill: transparent;
    stroke: transparent;
    stroke-width: 10px;

    @each $side in $sides {
        &.side#{map.get($side, index)} {
            fill: map.get($side, color);
        }
    }
}
@keyframes Field_last {
    from {stroke-width: 1px}
    to {stroke-width: 3px}
}
circle.last {
    fill: white;
    stroke: white;
    stroke-width: 3px;
    animation: Field_last 1s alternate infinite;
}
.paths {
    @each $side in $sides {
        .side#{map.get($side, index)} {
            stroke: map.get($side, color);
            stroke-width: 2px;
            fill: map.get($side, color);
            fill-opacity: .25;
        }
    }
}
.field {
  transform-origin: center;
}
.labels text, .label {
  font: 10px sans-serif;
  fill: gray;
}
.topLabels text {
  text-anchor: middle;
}
.bottomLabels text {
  text-anchor: middle;
  dominant-baseline: text-before-edge;
}
.leftLabels text {
  text-anchor: end;
  dominant-baseline: middle;
}
.rightLabels text {
  text-anchor: start;
  dominant-baseline: middle;
}

@keyframes Field_accDraw {
    from { color: rgba(0,0,0,1)}
    to { color: rgba(0,0,0,0)}
}
.Field_accDraw:not(:disabled) {
    animation: Field_accDraw 1s 4 alternate;
}

.Field_gnd {
    stroke: black!important;
    stroke-width: 1px!important;
}

.Field_notGnd {
    stroke: black!important;
    stroke-width: 2px!important;
    stroke-dasharray: 1px 1px!important;
}
.Field_free {
    stroke: black!important;
    stroke-width: 2px!important;
    stroke-dasharray: 3px 3px!important;
}
.Field_notGnd.Field_free {
    stroke-width: 4px!important;
}
@keyframes Field_errorHint {
    from { opacity: 100% }
    to { opacity: 0 }
}
.Field_errorHint {
    font-weight: bold;
    padding-left: 1em;
    color: $Red700;
    animation: Field_errorHint 2s;
}
.Field_move_warn {
    background-color: $Red50;
}
</style>