<template>
    <span :class="[$style.User, side !== null ? sideClass(side) : null, isMe && $style.User_me]">
        <span :class="$style.User__img" :style="user.pic && `background-image: url(up/${user.pic})`"></span>
        <span :class="[$style.User__level, levelClass(user.level)]">{{ levelIcon(user.level) }}</span>
        <span :class="$style.User__title">{{ user.title }}</span>
        <span v-if="showScore" :class="$style.User__score">{{ user.score }}</span>
    </span>
</template>

<script>
import {UserLevel} from "api";

export default {
    props: {
        user: Object,
        showScore: {
            type: Boolean,
            default: false
        },
        side: {
            type: Number,
            default: null
        }
    },
    computed: {
        isMe() {
            return this.$root.user && this.$root.user.id === this.user.id
        }
    },
    methods: {
        levelClass(level) {
            switch (level) {
                case 'N': return this.$style.User__levelN
                case 'L3': return this.$style.User__level3
                case 'L2': return this.$style.User__level2
                case 'L1': return this.$style.User__level1
                case 'C': return this.$style.User__levelC
                case 'M': return this.$style.User__levelM
                case 'GM': return this.$style.User__levelG
            }
        },
        levelIcon(level) {
            UserLevel
            switch (level) {
                case '3': return 'Ⅲ'
                case '2': return 'Ⅱ'
                case '1': return 'Ⅰ'
                default: return level
            }
        }
    }
}
</script>

<style lang="scss" module>

.User__img {
    box-sizing: border-box;
    width: 1.2em;
    height: 1.2em;
    display: inline-block;
    vertical-align: bottom;
    background-size: 100%;
    border-radius: 100%;

    @each $side in $sides {
        .side#{map.get($side, index)} & {
            box-shadow: 0 0 1px 1px map.get($side, color), 0 0 1px 1px map.get($side, color) inset;
        }
    }
}

.User__level {
    color: white;
    display: inline-block;
    margin-left: .3em;
    font-weight: bold;
    width: 1em;
    text-align: center;
}

.User__title {
    margin-left: .3em;
    .User_me & {
        font-weight: bold;
    }

}

.User__levelN { color: $LightBlue400 }
.User__level3 { color: $LightGreen400 }
.User__level2 { color: $LightGreen600 }
.User__level1 { color: $LightGreen800 }
.User__levelC { color: $Orange400 }
.User__levelM { color: $DeepOrange600 }
.User__levelG { color: $Red800 }
.User__score {
  margin-left: .1em;
  color: silver;
}

</style>