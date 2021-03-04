<template>
    <form @submit.prevent="submit" @reset.prevent="reset" :class="isDisabled && $cs.FDisabled || ''"><slot :$value="value" :$cont="this"/></form>
</template>

<script>
import {nextTick, reactive} from 'vue'
import {errorToString} from "../../api0";
import {setFocus} from "../../vue-autofocus";

export const contMixin0 = {
    provide() {
        return { fcont: this }
    },
    props: {
        disabled: Boolean,
    },
    computed: {
        isDisabled() {
            return this.disabled || this.parentCont && this.parentCont.isDisabled
        }
    },
    methods: {
        getRootCont(withPath) {
            let cont = this
            let path = ''
            while (cont.parentCont) {
                if (withPath) {
                    path = path ? cont.cName + "." + path : cont.cName
                }
                cont = cont.parentCont
            }
            return withPath ? {cont, path} : cont
        },
        add(arrName, value) {
            if (Array.isArray(this.value[arrName])) {
                this.value[arrName].push(value)
            } else {
                this.value[arrName] = [value]
            }
        },
        removeAt(arrName, index) {
            let arr = this.value[arrName];
            if (Array.isArray(arr)) {
                arr.splice(index, 1)
            }
        },
        remove(arrName, value) {
            if (arguments.length === 0) {
                this.parentCont.removeAt(this.name, this.index)
            } else {
                let arr = this.value[arrName];
                if (Array.isArray(arr)) {
                    for (let i = arr.length; i >= 0; i--) {
                        if (arr[i] === value) {
                            arr.splice(i, 1)
                        }
                    }
                }
            }
        },
    }
}

/**
 * Обработчик форм.
 * - Все элементы внутри `FForm` с атрибутом `name` становятся полями этой FForm.
 */
export default {
    mixins: [contMixin0],
    created() {
        this.parentCont = null
    },
    props: {
        /**
         * Объект, который будет хранить значения всех полей формы.
         */
        value: {
            type: Object,
            default() {  return reactive({}) }
        },
        onSubmit: Function,
        onReset: Function,
        noStrip: Boolean
    },
    data() {
        return {
            isSubmitting: false,
            els: [],
            errEls: [],
        }
    },
    computed: {
        isDisabled() {
            return this.disabled || this.isSubmitting
        }
    },
    methods: {
        strip() {
            // Make it deep
            let els = this.els
            let value = this.value
            values:
            for (let k in value) {
                if (value.hasOwnProperty(k)) {
                    for(let el of els) {
                        if (el.name === k || el.comp && el.comp.name === k) {
                            continue values
                        }
                    }
                    delete value[k]
                }
            }
        },
        getRootCont(withPath) {
            return withPath ? {cont: this, path: ''} : this
        },
        clearErrorFor(path, name) {
            for (let el of this.els) {
                if (el.path === path) {
                    if (el.name === name) {
                        if (el.el._ceCl) {
                            delete el.el._ceCl
                            el.el.classList.remove(this.$cs.FErroneous)
                        }
                    } else if (el.comp && el.comp.name === name) {
                        el.comp.erroneous = false
                    }
                }
            }
            for (let errEl of this.errEls) {
                if (errEl.path === path) {
                    errEl.comp.clearErrors(name)
                }
            }
        },
        async setErrors(errors) {
            console.log("FORM ERRORS", errors)
            let els = this.els;
            let errEls = this.errEls;
            for (let el of els) {
                if (el.el) {
                    if (el.el._ceCl) {
                        delete el.el._ceCl
                        el.el.classList.remove(this.$cs.FErroneous)
                    }
                } else if (el.comp) {
                    el.comp.erroneous = false
                }
            }
            for (let errEl of errEls) {
                errEl.comp.clearErrors()
            }
            if (errors && !Array.isArray(errors)) {
                if (typeof errors === 'object' && errors.error) {
                    if (errors.error === 'aborted') {
                        return
                    }
                    if (errors.error === 'errors') {
                        errors = errors.list
                    } else {
                        errors = [{message: errorToString(errors)}]
                    }
                } else {
                    errors = [{message: errorToString(errors)}]
                }
            }
            if (!errors || !errors.length) {
                return
            }
            errEls.sort((a, b) => a.comp.$el.compareDocumentPosition(b.comp.$el) & Node.DOCUMENT_POSITION_FOLLOWING ? -1 : 1)
            let commonErrors = []
            let erroneousEls = []
            errors:
            for(let error of errors) {
                let name = error.param
                if (!name) {
                    commonErrors.push(error)
                    continue
                }
                let path = ''
                let j = name.lastIndexOf('.')
                if (j >= 0) {
                    path = name.substr(0, j)
                    name = name.substr(j + 1)
                }
                // Находим элементы и расставляем признак
                for (let el of this.els) {
                    if (el.path === path) {
                        if (el.name === name) {
                            if (!el.el._ceCl) {
                                el.el._ceCl = true
                                el.el.classList.add(this.$cs.FErroneous)
                            }
                            erroneousEls.push(el)
                        } else if (el.comp && el.comp.name === name) {
                            el.comp.erroneous = true
                            erroneousEls.push(el)
                        }
                    }
                }
                // Ищем блоки для ошибок
                // по имени
                for (let errEl of errEls) {
                    if (errEl.path === path) {
                        let namesA = errEl.comp.namesA;
                        if (namesA && namesA.includes(name)) {
                            errEl.comp.addError(error)
                            continue errors
                        }
                    }
                }
                // Не нашли по имени, ищем элемент
                for (let el of els) {
                    if (el.path === path) {
                        let element
                        if (el.name === name) {
                            element = el.el
                        } else if (el.comp && el.comp.name === name) {
                            element = el.comp.$el
                        } else {
                            continue
                        }
                        // Теперь ищем FError, сразу следующий за элементом
                        for (let errEl of errEls) {
                            if (!errEl.comp.common && element.compareDocumentPosition(errEl.comp.$el) & Node.DOCUMENT_POSITION_FOLLOWING) {
                                errEl.comp.addError(error)
                                continue errors
                            }
                        }
                    }
                }
                // Не нашли, помещаем в общие ошибки
                commonErrors.push(error)
            }
            if (commonErrors.length) {
                for (let errEl of errEls) {
                    if (errEl.comp.common) {
                        errEl.comp.addErrors(commonErrors)
                        commonErrors.length = 0
                        break
                    }
                }
                if (commonErrors.length) {
                    // Не нашли где показать, показываем алерт
                    if (this.$msgErr) {
                        await this.$msgErr(commonErrors)
                    } else {
                        alert(commonErrors.map(e => e.param ? `${errorToString(e)} (${e.param})` : errorToString(e)).join('\n'))
                    }
                }
            }
            erroneousEls.sort((a, b) => (a.el || a.comp.$el).compareDocumentPosition(b.el || b.comp.$el) & Node.DOCUMENT_POSITION_FOLLOWING ? -1 : 1);
            for (let el of erroneousEls) {
                if (el.el) {
                    el.el.focus()
                    break
                } else if (el.comp.focus) {
                    el.comp.focus()
                    break
                } else if (el.comp.$el.focus) {
                    setFocus(el.comp.$el)
                    break
                }
            }
        },
        async submit(ev) {
            try {
                let active = document.activeElement;
                let submitter = ev.submitter
                if (submitter) {
                    submitter.toggleSubmitWait ? submitter.toggleSubmitWait(true) : submitter.classList.toggle(this.$cs.FSubmitWait, true)
                }
                this.isSubmitting = true
                try {
                    await this.setErrors(null)
                    if (!this.noStrip) {
                        this.strip()
                    }
                    if (this.onSubmit) {
                        return await this.onSubmit(this.value, ev)
                    }
                } finally {
                    this.isSubmitting = false
                    if (submitter) {
                        submitter.toggleSubmitWait ? submitter.toggleSubmitWait(false) : submitter.classList.toggle(this.$cs.FSubmitWait, false)
                    }
                    if (active && active.focus && active !== document.body && active !== document.documentElement) {
                        await nextTick()
                        active.focus()
                    }
                }
            } catch (e) {
                await nextTick()
                await this.setErrors(e)
            }
        },
        reset(ev) {
            if (this.onReset) {
                this.onReset(ev)
            }
        }
    }
}
</script>
