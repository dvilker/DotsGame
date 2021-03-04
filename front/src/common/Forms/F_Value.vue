<script>/**
 * Общие поля для контролов формы
 */
import {nextTick} from "vue";

export default {
    inheritAttrs: false,
    inject: ['fcont'],
    provide() {
        return { finput: this }
    },
    emits: ['update:modelValue'],
    props: {
        name: String,
        modelValue: null,
        disabled: Boolean,
    },
    data() {
        return {
            erroneous: false
        }
    },
    methods: {

        /**
         * Отправляет событие об изменении значения.
         */
        emitValue(value) {
            if (this.erroneous) {
                this.erroneous = false
                if (this.name) {
                    let {cont, path} = this.fcont.getRootCont(true)
                    cont.clearErrorFor(path, this.name)
                }
            }
            this.$emit('update:modelValue', value)
        },

        /**
         * Отправляет событие об изменении значения и устанавливает фокус на элемент.
         * Если [value] функция, то она вызывается с аргументом - текущим значением.
         */
        async setValue(value, select) {
            if (typeof value === "function") {
                value = await value(this.modelValue)
            }
            this.emitValue(value)
            await nextTick()
            this.focus(select)
        },
        focus(select) {
            let i = this.$refs.i;
            if (i) {
                i.focus()
                if (select) {
                    i.select()
                }
            }
        }
    },
    beforeMount() {
        if (this.fcont) {
            let {cont, path} = this.fcont.getRootCont(true)
            cont.els.push({
                comp: this,
                path
            })
        }
    },
    beforeUnmount() {
        if (this.fcont) {
            let cont = this.fcont.getRootCont()
            let arr = cont.els
            for (let i = arr.length - 1; i >= 0; i--) {
                if (arr[i].comp === this) {
                    arr.splice(i, 1)
                }
            }
        }
    },
}
</script>
