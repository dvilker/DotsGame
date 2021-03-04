<template>
    <FForm :class="$cs.cols" @submit="close(true)" @reset="close(false)" style="max-width: 600px">
        <template v-for="d of ds">
            <div v-if="d.orig.type==='confirm'">
                ⚠ {{ d.orig.message }}
            </div>
            <label v-else-if="d.orig.type==='yesno'">
                <input type="checkbox" :indeterminate.prop="d.value===undefined" v-model="d.value"
                       v-autofocus>
                {{ d.orig.message }}
            </label>
            <label v-else-if="d.orig.type==='captcha'" :class="$cs.cols">
                <FLabel :class="$cs.c6" caption="Число с картинки">
                    <FText v-model="d.value" v-autofocus/>
                </FLabel>
                <span :class="$cs.c6"><img :src="d.img" height="72" alt="Не робот ли вы?"></span>
            </label>
            <div v-else>
                <pre>{{ d }}</pre>
            </div>
        </template>
        <div :class="$cs.c8">
            <FButton submit :disabled="!allFilled" v-autofocus>Продолжить</FButton>
        </div>
        <div :class="$cs.c4">
            <FButton cancel>Отмена</FButton>
        </div>
    </FForm>
</template>

<script>
import FForm from "../Forms/form/FForm";
import FButton from "../Forms/FButton";
import FLabel from "../Forms/FLabel";
import FText from "../Forms/FText";
export default {
    components: {FText, FLabel, FButton, FForm},
    props: {
        dialogs: Array
    },
    emits: ['closeResolve'],
    data() {
        return {
            ds: this.dialogs.map(d => {
                let r = {
                    orig: d,
                    value: undefined
                }
                switch (d.type) {
                    case 'confirm':
                        r.value = 1
                        break;
                    case 'yesno':
                        r.value = typeof d.default === 'boolean' ? d.default : undefined
                        break;
                    case 'captcha':
                        let ci = d.ci.split(' ', 2)
                        r.c = ci[0]
                        r.img = ci[1]
                        break;
                }
                return r
            })
        }
    },
    computed: {
        allFilled() {
            return this.ds.every(d => d.value !== undefined)
        }
    },
    methods: {
        close(ok) {
            this.$emit(
                'closeResolve',
                ok
                    ? this.ds.map(d => ({
                        code: d.orig.code,
                        value: d.orig.type === 'captcha' ? d.value + ':' + d.c : d.value
                    }))
                    : null
            )
        }
    }
}
</script>

<style lang="scss" module>

</style>