<template>
<div :class="$cs.FInputBox" @keydown="keydown" @keyup="keyup" @focus="focus" @blur="focus"><slot/><slot name="btns"/></div>
</template>

<script>
/**
 * Рамка, для прямоугольных полей ввода с поддержкой DropDown и кнопок
 */
export default {
    provide() {
        return {fbox: this}
    },
    data() {
        return {
            btns: [],
            altDown: false
        }
    },
    methods: {
        keydown(e) {
            this.altDown = e.altKey;
            // Обработчик для Alt+? комбинаций для второстепенных элементов управления
            if (e.altKey && !e.ctrlKey && !e.shiftKey && e.key !== 'Alt') {
                for (let ch of this.btns) {
                    if (ch.altKey && typeof ch.altPressed === 'function') {
                        console.log("!!", ch.altKey)
                        if (ch.altKey === e.code || 'Key' + ch.altKey === e.code || 'Digit' + ch.altKey === e.code) {
                            console.log("Alt clicked", "!!!")
                            e.preventDefault();
                            ch.altPressed();
                        }
                    }
                }
            }
        },
        keyup(e) {
            this.altDown = e.altKey;
        },
        focus() {
            this.altDown = false;
        }
    }
}
</script>
