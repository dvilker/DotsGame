<template>
    <F_InputBox :class="[$cs.FText, erroneous ? $cs.FErroneous : '']" @wheel="wheel"><input
            :class="[$cs.FHandled, $cs.FInputBox_input]"
            ref="i"
            @input="emitValue(parseInt($event.target.value))"
            :disabled="disabled"
            :value="modelValue"
            type="text"
            v-bind="$attrs"
    /><template #btns><slot :Input="this"/>
        <FBtn altKey="Minus" @click="setValue(dec1)">−</FBtn>
        <FBtn altKey="Plus" @click="setValue(inc1)">+</FBtn>
    </template></F_InputBox>
</template>
<script>
import F_Value from "./F_Value";
import F_InputBox from "./F_InputBox";
import FBtn from "./FBtn";

export default {
    mixins: [F_Value],
    props: {
        min: {
            type: Number,
            default: null
        },
        max: {
            type: Number,
            default: null
        }
    },
    mounted() {
        this.$el.focus = () => this.$refs.i.focus()
    },
    components: {FBtn, F_InputBox},
    methods: {
        checkValue(v) {
            v = parseInt(v) || 0
            if (this.max !== null && v > this.max) {
                v = this.max
            }
            if (this.min !== null && v < this.min) {
                v = this.min
            }
            return v
        },
        inc1(v) {
            return (parseInt(v) || 0) + 1
        },
        dec1(v) {
            return (parseInt(v) || 0) - 1
        },
        wheel(e) {
            if (e.deltaX > 1 || e.deltaY > 1) {
                this.setValue(this.dec1)
            } else if (e.deltaX < 1 || e.deltaY < 1) {
                this.setValue(this.inc1)
            }
            e.preventDefault()
        }
    }
}
</script>
