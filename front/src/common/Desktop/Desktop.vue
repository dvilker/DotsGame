<template>
    <div>
        <slot/>
        <div v-if="windows && windows.length || topWindows && topWindows.length" :class="$style.Desktop_windows">
            <Window v-for="w of windows" :win="w"/>
            <Window v-for="w of topWindows" :win="w"/>
        </div>
        </div>
</template>

<script>
import Window from "./Window";
import {ComponentPublicInstance} from "@vue/runtime-core";

export default {
    components: {Window},
    props: {
        apiHandlers: Object
    },
    beforeCreate() {
        let desk = this
        let winIndex = 0;
        this.$root.$desktop = desk
        this.$.appContext.config.errorHandler = (err, vm, info) => {
            console.error("config.errorHandler", err, vm, info)
            desk.$msgErr(err)
        }
        this.$.appContext.app.mixin({
            beforeCreate() {
                this.$desktop = desk
            },
            methods: {
                $win(component, props) {
                    return new Promise((resolve, reject) => {
                        desk.windows.push({
                            component, props, resolve, reject, i: ++winIndex
                        })
                    })
                },
                $topWin(component, props, level) {
                    let win = {
                        component, props, i: ++winIndex, level: level || 0
                    }
                    win.close = () => {
                        let winIndex = desk.topWindows.findIndex(w => w.i === win.i)
                        if (winIndex >= 0) {
                            desk.topWindows.splice(winIndex, 1)
                        }
                    }
                    desk.topWindows.push(win)
                    desk.topWindows.sort((a, b) => b.level - a.level)
                },
                $msg(message) {
                    return this.$win(import("./MessageWin"), { message })
                },
                $msgErr(message) {
                    return this.$win(import("./MessageWin"), { message, error: true })
                },
                $modalWait(func) {
                    return this.$win(import("./WaitWin"), { waitFor: func })
                },
                $menu(menu, target) {

                }
            }
        })
        if (this.apiHandlers) {
            this.apiHandlers.onDialogs = async (data) => {
                let r = await this.$win(import("./DialogsWin"), {dialogs: data.dialogs})
                if (r) {
                    for (let d of data.dialogs) {
                        for (let v of r) {
                            if (d.code === v.code) {
                                d.value = v.value
                                break
                            }
                        }
                    }
                }
                return data.dialogs
            }
        }
    },
    data() {
        return {
            windows: [],
            topWindows: [],
        }
    },
    methods: {
        closeWindow(success, payload, window) {
            let winIndex = this.windows.findIndex(w => w.i === window.i)
            if (winIndex >= 0) {
                this.windows.splice(winIndex, 1)
            }
            (success ? window.resolve : window.reject)(payload)
        }
    }
}
</script>

<style lang="scss" module src="./desktop.module.scss"/>