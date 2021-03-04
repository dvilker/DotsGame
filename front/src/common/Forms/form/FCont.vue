<template><slot :$cont="fcont" :$value="fcont.value" /></template>

<script>
export default {
    inject: ['fcont'],
    computed: {
        parentCont() {
            return this.fcont.parentCont
        }
    }
}
</script>