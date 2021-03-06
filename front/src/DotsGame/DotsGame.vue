<template>
    <Desktop :apiHandlers="apiHandlers">
        <div :class="$style.DG_main">
            <div :class="[$style.DG_left, $cs.cols, $cs.rows]">
                <UserList :class="$style.DG_list"/>
                <Chat v-if="activeRoom" :class="$style.DG_chat"/>
            </div>
            <Field
                v-if="activeBattle"
                :class="$style.DG_center"
                :battle="activeBattle"
                :key="activeBattle.id"
            />
            <div v-else :class="$style.DG_center">Пригласите кого-нибудь поиграть.</div>
        </div>
    </Desktop>
    <div v-if="!connected" :class="$style.DG_ban">
        Нет соединения с сервером. <FButton @click="reconnect">↻</FButton>
    </div>
</template>

<script>
import Desktop from '../common/Desktop/Desktop'
import Field from './Field/Field'
import UserList from './UserList'
import Chat from './Chat'
import {
    addConnectionEventListener,
    apiHandlers,
    enumFromName,
    removeConnectionEventListener,
    wsConnect
} from "../common/api0";
import {RuleSize, RuleStart, RuleTimer, selectRoom} from "api";
import {getUser} from "./Users";
import FButton from "../common/Forms/FButton";
import {delay} from "../common/misc";

export default {
    components: {FButton, Desktop, Field, UserList, Chat},
    beforeCreate() {
        this.apiHandlers = apiHandlers
        apiHandlers.onConnectChanged = (connected, payload) => {
            this.connected = connected
            if (connected) {
                this.offers = []
                this.battles = []
                this.activeBattle = null
                this.activeRoom = null
                this.user = payload && payload.user && getUser(payload.user) || null
                this.online = payload.online.map(u => getUser(u))
                console.log("USERS onConnectChanged", this.online.map(it=>it.title))
                console.log("USERS onConnectChanged", this.online.map(it=>it.id))
            } else {
                // this.user = null
            }
        }
        apiHandlers.onLatencyChanged = (latency) => {
            this.latency = latency
        }
        this._cel = ev => {
            switch (ev._) {
                case 'user+': {
                    console.log("USERS BF CONNECT", this.online.map(it=>it.title))
                    this.online.push(getUser(ev.user))
                    console.log("USERS AF CONNECT", this.online.map(it=>it.title))
                    break;
                }
                case 'user-': {
                    console.log("USERS BF DISCONNECT", this.online.map(it=>it.title))
                    for (let i = this.online.length - 1; i >= 0; i--) {
                        if (this.online[i].id === ev.user.id) {
                            console.log("DISCONNECTED", this.online[i])
                            this.online.splice(i, 1)
                            break
                        }
                    }
                    console.log("USERS AF DISCONNECT", this.online.map(it=>it.title))
                    break;
                }
                case 'user': {
                    getUser(ev.user)
                    break
                }
                case 'offer+': {
                    for (let offer of ev.offers) {
                        // remove
                        for(let i=this.offers.length-1; i>=0; i--) {
                            if (this.offers[i].side0 === offer.side0 && this.offers[i].side1 === offer.side1) {
                                this.offers.splice(i, 1)
                            }
                        }
                        // and add
                        offer.users = [getUser(offer.side0), getUser(offer.side1)]
                        this.offers.push(offer)
                    }
                    break
                }
                case 'offer-': {
                    let offer = ev.offer
                    for(let i=this.offers.length-1; i>=0; i--) {
                        if (this.offers[i].side0 === offer.side0 && this.offers[i].side1 === offer.side1) {
                            this.offers.splice(i, 1)
                        }
                    }
                    break
                }
                case 'battle+': {
                    battles: for (let battle of ev.battles) {
                        battle.users = [getUser(battle.side0), getUser(battle.side1)]
                        // remove
                        for(let i=this.battles.length-1; i>=0; i--) {
                            if (this.battles[i].id === battle.id) {
                                this.battles.splice(i, 1, battle)
                                continue battles
                            }
                        }
                        // and add, if not added
                        this.battles.push(battle)
                    }
                    break
                }
                case 'battle-': {
                    // remove
                    for(let i=this.battles.length-1; i>=0; i--) {
                        if (this.battles[i].id === ev.battleId) {
                            this.battles.splice(i, 1)
                        }
                    }
                    break
                }
                case 'battle': {
                    let battle = ev.battle
                    battle.users = [getUser(battle.side0.userId), getUser(battle.side1.userId)]
                    battle.rules.start = enumFromName(RuleStart, battle.rules.start)
                    battle.rules.size = enumFromName(RuleSize, battle.rules.size)
                    battle.rules.timer = enumFromName(RuleTimer, battle.rules.timer)
                    battle.moves = ''
                    this.activeBattle = battle
                    break
                }
                case 'battleMove': {
                    if (this.activeBattle && this.activeBattle.id === ev.battleId) {
                        if (this.activeBattle.moves.length === ev.offset) {
                            this.activeBattle.moves += ev.moves
                        } else {
                            console.error("Рассинхронизация ходов")
                        }
                        this.activeBattle.moveSide = ev.moveSide
                        this.activeBattle.side0.points = ev.p0
                        this.activeBattle.side1.points = ev.p1
                        this.activeBattle.side0.totalTime = ev.t0
                        this.activeBattle.side1.totalTime = ev.t1
                        this.activeBattle.moveStartTime = ev.moveStartTime
                        this.activeBattle.moveStartTimeLocal = ev.moveStartTime - ev.now + Date.now()
                    }
                    break
                }
                case 'battleOver': {
                    if (this.activeBattle && this.activeBattle.id === ev.battleId) {
                        this.activeBattle.over = ev.over
                        this.activeBattle.askDraw = null
                    }
                    break
                }
                case 'battleDraw': {
                    if (this.activeBattle && this.activeBattle.id === ev.battleId) {
                        this.activeBattle.askDraw = ev.askDraw
                    }
                    break
                }
                case 'room': {
                    let room = ev.room
                    let messages = ev.messages
                    for (let m of messages) {
                        m.from = m.from && getUser(m.from)
                        m.to = m.to && m.to.map(u => getUser(u))
                    }
                    room.messages = ev.messages
                    this.activeRoom = room
                    break
                }
                case 'msg': {
                    if (this.activeRoom && this.activeRoom.id === ev.roomId) {
                        let m = ev.msg
                        m.from = m.from && getUser(m.from)
                        m.to = m.to && m.to.map(u => getUser(u))
                        this.activeRoom.messages.push(m)
                    }
                    break
                }
            }
            //console.log("EVEVEV", ev)
        }
        addConnectionEventListener(this._cel)
        wsConnect()
    },
    beforeUnmount() {
        removeConnectionEventListener(this._cel)
    },
    methods: {
        async selectRoom(roomOrBattle) {
            let roomId = roomOrBattle.roomId || roomOrBattle.id
            await selectRoom({roomId})
            // this.activeBattleId = b.id
            // this.activeBattleMoves = null
            // await selectBattle({battleId: b.id})
        },
        async reconnect() {
            let time = Date.now()
            try {
                await wsConnect(true)
            } catch (e) {
                if (e && e.isSocketConnectionError) {
                    time = 1000 - (Date.now() - time)
                    if (time > 0) {
                        await delay(time)
                    }
                } else {
                    throw e
                }
            }
        }
    },
    data() {
        return {
            connected: false,
            latency: null,

            user: null,
            online: [],
            offers: [],
            battles: [], // battle headers
            activeBattle: null,
            activeRoom: null,
        }
    },
    computed: {
        sortedOnline() {
            return this.online.sort((a, b) => b.score - a.score)
        }
    }
}
</script>

<style lang="scss" module>
html, body {
    height: 100%;
    padding: 0;
    margin: 0;
    background: whitesmoke;
    max-height: 800px;
}
.DG_main {
    width: 1140px;
    min-height: 100%;
    display: flex;
    margin: auto;
    > * {
        flex-shrink: 0;
        flex-grow: 0;
        background-color: white;
    }
}
.DG_left {
    width: 300px;
    box-sizing: border-box;
}
.DG_center {
    width: 840px;
    box-sizing: border-box;
}

.DG_list {
    &:last-child {
        height: 100%;
    }
    &:not(:last-child) {
        height: 50%;
    }
}

.DG_chat {
    max-width: 300px;
    height: 50%;
}
.DG_ban {
    position: fixed ;
    left: 50%;
    transform: translateX(-50%);
    top: 0;
    z-index: 99999;
    background-color: white;
    opacity: .9;
    padding: calc(var(--cGap) / 2) var(--cGap);
    box-shadow: 0 0 1em rgba(0,0,0,.25)
}
</style>