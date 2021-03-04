<template><I
        v-for="($value, $index) of value"
        :name="name"
        :value="$value"
        :index="$index"
        #="I"
><slot :$index="$index" :$value="$value" :$cont="I" :$parentCont="I.parentCont"/></I></template>

<script>
import {contMixin0} from "./FForm";

export default {
    components: {
        I: {
            mixins: [contMixin0],
            inject: {
                parentCont: 'fcont'
            },
            provide() {
                return { fcont: this }
            },
            props: {
                value: {},
                index: Number,
                name: {}
            },
            computed: {
                cName() {
                    return this.name + "." + this.index
                }
            },
            render() {
                return this.$slots.default(this)
            }
        }
    },
    props: {
        value: {},
        name: {}
    }
}
</script>