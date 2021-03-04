<template><slot v-if="typeof value === 'object'" :$cont="this" :$parentCont="parentCont" :$value="value" /></template>

<script>
import {contMixin0} from "./FForm";

export default {
    inject: {
        parentCont: 'fcont'
    },
    provide() {
        return { fcont: this }
    },
    emits: ['setDefault'],
    mixins: [contMixin0],
    watch: {
        value: {
            immediate: true,
            handler(v) {
                if (typeof v !== 'object' && typeof this.default === 'object') {
                    this.$emit('setDefault', this.default)
                }
            }
        }
    },
    props: {
        name: {},
        value: {},
        default: {}
    },
    computed: {
        cName() {
            return this.name
        }
    }
}
</script>