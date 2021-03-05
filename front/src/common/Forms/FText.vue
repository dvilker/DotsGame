<template>
    <F_InputBox :class="[$cs.FText, erroneous ? $cs.FErroneous : '']"><input
            :class="[$cs.FHandled, $cs.FInputBox_input]"
            ref="i"
            @input="emitValue($event.target.value)"
            :disabled="disabled"
            :value="modelValue"
            :type="password ? 'password' : 'text'"
            v-bind="$attrs"
    /><template #btns><slot :Input="this"/><FBtn altKey="X" @click="setValue('')" title="Очистить">&times;</FBtn></template></F_InputBox>
</template>
<script>
import F_Value from "./F_Value";
import F_InputBox from "./F_InputBox";
import FBtn from "./FBtn";

export default {
    mixins: [F_Value],
    props: {
        password: Boolean
    },
    mounted() {
        this.$el.focus = () => this.$refs.i.focus()
    },
    components: {FBtn, F_InputBox}
}
</script>
