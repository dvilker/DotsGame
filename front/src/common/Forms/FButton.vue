<template>
    <button
            :class="[$cs.FButton, cancel ? $cs.FCancel : '', wait ? $cs.FSubmitWait : '']"
            :type="submit ? 'submit' : cancel ? 'reset' : 'button'"
            :disabled="disabled || wait"
            @click="click"
    ><slot/></button>
</template>

<script>
export default {
    props: {
        submit: Boolean,
        cancel: Boolean,
        disabled: Boolean,
        onClick: Function
    },
    mounted() {
        this.$el.toggleSubmitWait = w => w ? this.wait++ : this.wait--
    },
    data() {
        return {
            wait: 0
        }
    },
    methods: {
        async click(ev) {
            if (this.onClick) {
                this.wait++
                try {
                    return await this.onClick(ev)
                } finally {
                    this.wait--
                }
            }
        }
    }
}
</script>
