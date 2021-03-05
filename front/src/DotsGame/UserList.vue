<template>
<div :class="$style.UserList">
    <div :class="$style.users">
<!--        <div>
            <span>Главная</span>
        </div>-->
<!--        <div>
            <span>Зал славы</span>
        </div>
        <div>
            <span>Оконченные партии</span>
        </div>
        <div>
            <span>Тренировка</span>
        </div>-->
        <div v-if="$root.user">
<!--            <User :user="$root.user"/>-->
            <FButton @click="!$win(import('./LoginWin'))">Профиль</FButton>
        </div>
        <div v-else><FButton @click="!$win(import('./LoginWin'))">Войдите, чтобы поиграть&hellip;</FButton></div>

<!--        <div v-for="battle of $root.battles" :class="$style.UserList__game">
            <template v-if="$root.user && battle.state === 'OFFER'">
                <template v-if="battle.sides[0].user.id === $root.user.id">
                    Вы приглашаете
                    <template v-for="(s, si) of battle.sides">
                        <User v-if="s.user.id !== $root.user.id" :user="s.user" :side="si"/>
                    </template>
                </template><template v-else>
                    Вас приглашают
                    <template v-for="(s, si) of battle.sides">
                        <User v-if="s.user.id !== $root.user.id" :user="s.user" :side="si"/>
                    </template>
            </template>
            </template><template v-else>
                <User v-for="(s, si) of battle.sides" :user="s.user" :side="si"/>
            </template>
        </div>
        <div v-for="g of $root.games" :class="$style.UserList__game">
            <User v-for="(s, si) of g.sides" :user="s.user" :side="si"/>
        </div>-->
        <div>
            <table :class="$style.UserList_table">
                <template v-if="income.length">
                    <tr><td colspan="3">Вас приглашают сыграть</td></tr>
                    <tr v-for="offer of income">
                        <td>
                            <template v-for="(user, si) of offer.users">
                                <User v-if="user.id !== $root.user.id" :user="user" :side="si"/>
                            </template>
                        </td>
                        <td>
                            <template v-for="(user, si) of offer.users">
                                <template v-if="user.id !== $root.user.id">
                                    {{user.score}}
                                </template>
                            </template>
                        </td>
                        <td><FButton @click="acceptOffer(offer)">В игру</FButton></td>
                    </tr>
                </template>
                <template v-for="battle of $root.battles">
                    <tr :class="[$style.UserList__game, battle.isOver && $style.UserList_over]">
                        <td>
                            <User v-for="(s, si) of battle.users" :user="s" :side="si"/>
                        </td>
                        <td>
                            <div v-for="(s, si) of battle.users">{{s.score}}</div>
                        </td>
                        <td>
                            <FButton :disabled="$root.activeRoom && $root.activeRoom.id === battle.roomId" @click="selectRoom(battle)">→</FButton>
                        </td>
                    </tr>
                </template>
                <tr v-for="u of $root.sortedOnline">
                    <td><User :user="u" /></td>
                    <td>{{u.score}}</td>
                    <td width="1" :style="{visibility: $root.user && u.id === $root.user.id ? 'hidden' : undefined}">
                        <template v-if="outcome.includes(u.id)">
                            <FButton @click="toggleOffer(u)">Отбой</FButton>
                        </template><template v-else>
                            <FButton @click="toggleOffer(u)">Играть</FButton>
                        </template>
                    </td>
                </tr>
            </table>
        </div>
    </div>
</div>
</template>

<script>

import User from "./User";
import FButton from "../common/Forms/FButton";
import {acceptOffer, toggleOffer} from "api";
import {getUser} from "./Users";
import FEnumSelect from "../common/Forms/FEnumSelect";
export default {
    components: {FEnumSelect, FButton, User},
    methods: {
        getUser,
        async toggleOffer(u) {
            await toggleOffer({withUserId: u.id})
        },
        async acceptOffer(offer) {
            await acceptOffer({withUserId: offer.side0})
        },
        async selectRoom(b) {
            await this.$root.selectRoom(b)
        }
    },
    computed: {
        income() {
            let income = []
            if (this.$root.user) {
                for (let offer of this.$root.offers) {
                    if (offer.side1 === this.$root.user.id) {
                        income.push(offer)
                    }
                }
            }
            return income
        },
        outcome() {
            let outcome = []
            if (this.$root.user) {
                for (let offer of this.$root.offers) {
                    if (offer.side0 === this.$root.user.id) {
                        outcome.push(offer.side1)
                    }
                }
            }
            return outcome
        }
    }
}
</script>

<style lang="scss" module>
.UserList {
  overflow-y: auto;
}
.UserList_table {
    width: 100%;
    td {
        padding: .3em .1em;
    }
    td:not(:first-child) {
        width: 1px;
        text-align: right;
    }
    td .FButton{
        width: 100%;
    }
    .UserList__game td:first-child > *{
        display: block;
    }
}

.UserList_over {
    font-style: italic;
}


//.users > * {
//  padding: .3em;
//  display: block;
//}

//.UserList__game {
//    /*padding: .3em;*/
//    > * {
//        display: block;
//    }
//}
</style>