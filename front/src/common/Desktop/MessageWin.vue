<template>
    <div :class="$style.MessageWin">
        <template v-if="msg.message">
            <div v-if="msg.isHtml" v-html="msg.message" :class="$style.MessageWin_message"></div>
            <div v-else :class="$style.MessageWin_message">{{ msg.message }}</div>
        </template>
        <div :class="$style.MessageWin_buttons">
            <FButton v-for="(b, bi) of msg.buttons" :key="bi" @click="click(b)" :cancel="b.cancel" v-autofocus="b.default ? 1 : 0">{{ b.caption }}</FButton>
        </div>
    </div>
</template>

<script>
import {errorToString} from "../api0";
import FButton from "../Forms/FButton";

export const defaultButtonCaptions = {
    ok: 'ОК',
    cancel: 'Отмена',
    yes: 'Да',
    no: 'Нет',
    close: 'Закрыть',
    retry: 'Повторить'
};

export const OK_BUTTON = [{caption: defaultButtonCaptions.ok, result: 'ok', default: true}]
export const YES_NO_BUTTON = [{caption: defaultButtonCaptions.yes, result: 'yes', default: true}, {caption: defaultButtonCaptions.no, result: 'no', cancel: true}]

export default {
    components: {FButton},
    emits: ['closeResolve'],
    props: {
        message: {},
        error: Boolean
    },
    computed: {
        msg() {
            let message = this.message
            if (this.error) {
                if (typeof message === 'string' || Array.isArray(message)) {
                    message = { message }
                }
                if (Array.isArray(message.message)) {
                    message.message = message.message.join('\n');
                }
                message.type = message.type || 'error';
            }
            if (typeof message === 'string' || typeof message === 'object' && message.error || message instanceof Error) {
                message = { message }
            }
            if (typeof message.message === 'object' && message.message.error) {
                message.message = errorToString(message.message)
            }
            if (!message.buttons || Array.isArray(message.buttons) && message.buttons.length === 0) {
                message.buttons = OK_BUTTON
            } else {
                if (typeof message.buttons === 'string') {
                    // "+ok:ОК\n-cancel:Отмена"
                    let buttons = [];
                    for (let btnStr of message.buttons.split("\n")) {
                        let btnSplit = split(btnStr, ':', 2);
                        let name, caption = null;
                        if (btnSplit.length === 1) {
                            name = btnSplit[0];
                        } else {
                            name = btnSplit[0];
                            caption = btnSplit[1];
                        }
                        let def = false, cancel = false;
                        if (name.substr(0, 1) === '+') {
                            def = true;
                            name = name.substr(1);
                        } else if (name.substr(0, 1) === '-') {
                            cancel = true;
                            name = name.substr(1);

                        }
                        if (!caption) {
                            caption = defaultButtonCaptions[name];
                        }
                        buttons.push({
                            caption: caption || name,
                            result: name,
                            default: def,
                            cancel: cancel
                        })
                    }
                    message.buttons = buttons;
                }
            }
            if (message.buttons.length === 1) {
                message.buttons[0].default = true;
                message.buttons[0].cancel = true;
            } else {
                let hasCancel = false;
                let cancelBtn = null;
                for (let btn of message.buttons) {
                    if (btn.cancel) {
                        hasCancel = true;
                        break;
                    } else if (btn.result === 'cancel') {
                        cancelBtn = btn;
                    }
                }
                if (!hasCancel && cancelBtn) {
                    cancelBtn.cancel = true;
                }
            }
            return message
        }
    },
    methods: {
        click(button) {
            this.$emit('closeResolve', {result: button.result, button});
        },
    }
}
</script>

<style lang="scss" module>
.MessageWin {
    padding: var(--cGap);
}
.MessageWin_message {
    white-space: pre-line;
}
.MessageWin_buttons {
    text-align: center;
    >:not(:first-child) {
        margin-left: var(--cGap);
    }
    >* {
        width: auto;
        min-width: 100px;
    }
    margin-top: var(--cGap);
}
</style>