<template>
<div :class="$style.DesktopWindow">
    <div>
        <div v-if="$asyncComputed.cmp.updating || $asyncComputed.prp.updating">
            &hellip;
        </div>
        <div v-else-if="$asyncComputed.cmp.error || $asyncComputed.prp.error">
            Ошибка:
            {{ $asyncComputed.cmp.exception }}
            {{ $asyncComputed.prp.exception }}
        </div>
        <component v-else
                   :is="cmp"
                   v-bind="prp"
                   @closeResolve="$parent.closeWindow(true, $event, win)"
                   @closeReject="$parent.closeWindow(false, $event, win)"
        />
    </div>
</div>
</template>

<script>
export default {
    props: {
        win: Object,
    },
    asyncComputed: {
        async cmp() {
            let cmp = this.win.component
            if (typeof cmp === "function") {
                cmp = await cmp()
            }
            cmp = await cmp
            if (cmp.__esModule) {
                cmp = cmp.default
            }
            return cmp
        },
        async prp() {
            let prp = this.win.props
            if (typeof prp === "function") {
                prp = await prp()
            }
            prp = await prp
            if (prp && prp.__esModule) {
                prp = prp.default
            }
            return prp
        }
    }
}
</script>

<style lang="scss" module src="./desktop.module.scss"/>