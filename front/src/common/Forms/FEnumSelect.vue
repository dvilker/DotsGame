<template>
    <F_InputBox :class="[$cs.FText, erroneous ? $cs.FErroneous : '']"><select
            :class="[$cs.FHandled, $cs.FInputBox_input]"
            ref="i"
            @input="input"
            :disabled="disabled"
            v-bind="$attrs"
    ><option
            v-for="(o, oi) of options"
            :key="oi"
            :selected="o.name === modelValue || o.name === null && (modelValue === null || modelValue === undefined)"
            :class="[o.$unk$ && $cs.FSelect_unk, o.$null$ && $cs.FSelect_null]"
    >{{ o.title }}</option></select><template #btns><slot :Input="this"/></template></F_InputBox>
</template>
<script>
import F_Value from "./F_Value";
import F_InputBox from "./F_InputBox";
import FBtn from "./FBtn";

export default {
    emits: ['item'],
    mixins: [F_Value],
    props: {
        enum: Object,
        nullTitle: String
    },
    mounted() {
        this.$el.focus = () => this.$refs.i.focus()
        if (this.modelValue) {
            let select = this.$refs.i;
            let index = select.selectedIndex;
            if (index >= 0) {
                this.$emit('item', this.options[index])
            }
        }
    },
    methods: {
        input(ev) {
            let select = ev.target;
            let index = select.selectedIndex;
            if (index >= 0) {
                this.emitValue(this.value)
                this.setValue(this.options[index].name);
                this.$emit('item', this.options[index])
            } else {
                this.setValue(null);
                this.$emit('item', null)
            }
        }
    },
    computed: {
        options() {
            let r = [], e = this.enum, hasValue = false
            if (this.nullTitle) {
                if (this.modelValue === null) {
                    hasValue = true
                }
                r.push({
                    name: null,
                    title: this.nullTitle
                })
            }
            for (let k in e) {
                if (e.hasOwnProperty(k)) {
                    if (k === this.modelValue) {
                        hasValue = true
                    }
                    r.push(e[k])
                }
            }
            if (!hasValue) {
                r.unshift({
                    name: this.modelValue,
                    title: String(this.modelValue || '?')
                })
            }
            return r
        }
    },
    components: {FBtn, F_InputBox}
}
</script>
