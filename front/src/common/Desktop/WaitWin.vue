<template>
    <div :class="$style.WaitWin">{{ message }}</div>
</template>

<script>
import {delay} from "../misc";

export default {
    emits: ['closeResolve', 'closeReject'],
    props: {
        waitFor: {}
    },
    data() {
        return {
            message: '...'
        }
    },
    async mounted() {
        // TODO разобраться как надежно показать ожидание
        await delay(55)
        document.documentElement.offsetHeight // force update
        let promiseOrFunction = this.waitFor
        try {
            this.$emit('closeResolve', await(typeof promiseOrFunction === 'function' ? promiseOrFunction(this) : promiseOrFunction))
        } catch (e) {
            this.$emit('closeReject', e)
        }
    },
}
</script>

<style lang="scss" module>
.WaitWin {
    text-align: center;
    &:before {
        content: '';
        display: block;
        min-width: 3em;
        min-height: 3em;
        background: var(--wait-image) center no-repeat;
        background-size: contain;
    }
}
</style>