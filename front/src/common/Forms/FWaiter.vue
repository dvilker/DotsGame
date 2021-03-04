<template>
    <div v-if="ac.updating">
        &hellip;
    </div><div v-else-if="ac.error" :class="$cs.FError">
        Ошибка: {{ errorToString(ac.error) }}
        <br><br><FButton @click="ac.update()">Повтор</FButton>
    </div><template v-else><slot/></template>
</template>

<script>
import {errorToString} from "../api0";
import FButton from "./FButton";

export default {
    components: {FButton},
    props: {
        ac: Object
    },
   /* computed: {
        updating() {
            let ac = this.ac
            if (Array.isArray(ac)) {
                return ac.some(a=>a.updating)
            } else {
                ac.updating
            }
        },
        error() {
            let ac = this.ac
            if (Array.isArray(ac)) {
                return ac.some(a=>a.error)
            } else {
                ac.updating
            }
        }
    },*/
    methods: {
        errorToString
    },
}
</script>
