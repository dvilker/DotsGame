<template>
    <Desktop :class="$style.bodyRoot" :apiHandlers="apiHandlers" style="min-height: 100%">
        <div class="cols rows">
            <UserList :class="$style.bodyUserList"/>
            <Chat v-if="activeRoom" :class="$style.bodyChat"/>
        </div>
        <Field v-if="activeBattle" :battle="activeBattle" :key="activeBattle.id" :class="$style.bodyField"/>
        <div v-if="!connected" style="position: absolute; opacity: .9; padding: calc(var(--cGap) / 2) var(--cGap); box-shadow: 0 0 1em rgba(0,0,0,.25)">
            Нет соединения с сервером. <FButton @click="reconnect">↻</FButton>
        </div>
    </Desktop>
</template>

<script>
import Desktop from '../common/Desktop/Desktop'
import Field from './Field/Field'
import UserList from './UserList'
import Chat from './Chat'
import {addConnectionEventListener, apiHandlers, removeConnectionEventListener, wsConnect} from "../common/api0";
import {selectRoom} from "api";
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
            console.log("EVEVEV", ev)
        }
        addConnectionEventListener(this._cel)
        // apiHandlers.onAuth = async () => {
        //     if (await this.$win(import('./LoginWin'))) {
        //         return Promise.reject(null)
        //     }
        // }
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
}

.bodyRoot {
    display: flex;
    justify-content: center;
    flex-wrap: nowrap;
    max-height: 100%;

    > * {
        background-color: white;
    }
}

.bodyField {

}

.bodyUserList {
    width: 300px;
    &:last-child {
        height: 100%;
    }
    &:not(:last-child) {
        height: 50%;
    }
}

.bodyChat {
    width: 300px;
    max-width: 300px;
    height: 50%;
}
</style>