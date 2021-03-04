<template>
<button
        type="button"
        :disabled="disabled || finput.disabled"
        tabindex="-1"
        @click="click"
        :class="$cs.FHandled"
        :title="title0"
><slot/><span v-if="altKey && fbox.altDown" :class="$cs.FBtn_key"><span>{{altKey}}</span></span></button>
</template>

<script>
export default {
    emits: ['click'],
    inject: ['finput', 'fbox'],
    props: {
        altKey: {
            type: String,
            default: null
        },
        disabled: Boolean,
        title: String
    },
    data() {
        return {
            activeTimer: null,
            active: false
        }
    },
    beforeMount() {
        this.fbox.btns.push(this)
    },
    beforeUnmount() {
        let arr = this.fbox.btns
        for(let i = arr.length - 1; i >= 0; i--) {
            if (arr[i] === this) {
                arr.splice(i, 1)
            }
        }
    },
    computed: {
        title0() {
            if (this.title && this.altKey) {
                return this.title + ' (Alt + ' + this.altKey + ')';
            } else if (this.title) {
                return this.title
            } else if (this.altKey) {
                return 'Alt + ' + this.altKey;
            } else {
                return null;
            }
        },
    },
    methods: {
        click() {
            this.finput.focus();
            this.$emit('click', this.finput);
        },
        altPressed() {
            if (this.disabled || this.finput.disabled) {
                return;
            }
            if (this.activeTimer) {
                clearTimeout(this.activeTimer);
            }
            this.active = true;
            this.activeTimer = setTimeout(() => {
                this.active = false;
                this.activeTimer = null;
            }, 100);
            this.finput.focus();
            this.$emit('click', this.finput);
        }
    }

}
</script>
