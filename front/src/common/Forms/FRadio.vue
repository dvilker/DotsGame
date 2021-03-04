<template>
    <label :class="$cs.FRadio"><input type="radio"
          :class="$cs.FHandled"
           ref="i"
           @input="$event.target.checked && emitValue(this.value)"
           :disabled="disabled"
           :checked="checked"
    ><slot>{{ caption }}</slot></label>
</template>
<script>
import F_Value from "./F_Value"

export default {
    mixins: [F_Value],
    props: {
        caption: String,
        value: null,
    },
    mounted() {
        this.$el.focus = () => this.$refs.i.focus()
    },
    computed: {
        checked() {
            return this.modelValue === this.value
        }
    },
}
</script>
