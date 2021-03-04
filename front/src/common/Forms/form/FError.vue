<template>
    <slot :$err="this"><div v-if="errors.length" :class="$cs.FError"><div v-for="err of errors">{{ err.message }}</div></div></slot>
</template>

<script>
export default {
    inject: ['fcont'],
    props: {
        names: null,
        common: Boolean
    },
    beforeMount() {
        let cont = this.fcont
        let path = []
        while (cont.parentCont) {
            path.unshift(cont.cName)
            cont = cont.parentCont
        }
        cont.errEls.push({
            comp: this,
            path: path.join('.')
        })
    },
    beforeUnmount() {
        let cont = this.fcont
        while (cont.parentCont) {
            cont = cont.parentCont
        }
        let arr = cont.errEls
        for(let i = arr.length - 1; i >= 0; i--) {
            if (arr[i].comp === this) {
                arr.splice(i, 1)
            }
        }
    },
    data() {
        return {
            errors: []
        }
    },
    computed: {
        namesA() {
            if (!this.names) {
                return null
            }
            if (Array.isArray(this.names)) {
                return this.names
            }
            return this.names.trim().split(/\s+/)
        }
    },
    methods: {
        clearErrors(name) {
            if (name) {
                let e = this.errors
                for (let i = e.length - 1; i >= 0; i--) {
                    if (e[i].param === name || e[i].param.endsWith('.' + name)) {
                        e.splice(i, 1)
                    }
                }
            } else {
                this.errors.length = 0
            }
        },
        addError(error) {
            this.errors.push(error)
        },
        addErrors(errors) {
            this.errors.push(...errors)
        },
    }
}
</script>
