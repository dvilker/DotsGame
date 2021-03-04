<template>
    <F_InputBox :class="[erroneous ? $cs.FErroneous : '']">
        <input type="file" accept="image/jpeg, image/png" style="width: 0" ref="i" @change="iChanged">
        <div :class="$cs.FInputBox_input">
            <img style="vertical-align: top" :src="modelValue" :class="{[$cs.FPic_circle]:circle}"/>
        </div>
        <template #btns><slot :Input="this"/><FBtn altKey="E" @click="selectPic()">Выбрать</FBtn><FBtn altKey="X" @click="setValue('')" title="Очистить">&times;</FBtn></template>
    </F_InputBox>
</template>
<script>
import F_Value from "./F_Value";
import F_InputBox from "./F_InputBox";
import FBtn from "./FBtn";

export default {
    mixins: [F_Value],
    props: {
        picSize: {
            type: Number,
            default: 96
        },
        maxLength: Number,
        circle: Boolean
    },
    mounted() {
        this.$el.focus = () => this.$refs.i.focus()
    },
    components: {FBtn, F_InputBox},
    methods: {
        async iChanged(ev) {
            try {
                let file = this.$refs.i.files
                file = file && file[0]
                if (file) {
                    console.log("File", file)

                    let image = await new Promise(async (resolve, reject) => {
                        let image = new Image()
                        image.onload = () => resolve(image)
                        image.onerror = reject
                        image.src = await new Promise((resolve, reject) => {
                            let reader = new FileReader()
                            reader.onload = function (ev) {
                                resolve(ev.target.result)
                            }
                            reader.onerror = reject
                            reader.readAsDataURL(file)
                        })
                    })
                    let picSize = this.picSize

                    let canvas = document.createElement('canvas')
                    canvas.width = picSize
                    canvas.height = picSize
                    let context = canvas.getContext('2d')
                    context.imageSmoothingQuality = "high"
                    context.drawImage(
                        image,
                        Math.max((image.width - image.height) / 2, 0),
                        Math.max((image.height - image.width) / 2, 0),
                        Math.min(image.width, image.height),
                        Math.min(image.width, image.height),
                        0, 0, picSize, picSize)

                    let targetSize = this.maxLength
                    let quality = 0.92
                    let jpeg = canvas.toDataURL('image/jpeg', quality)
                    if (targetSize && jpeg.length > targetSize) {
                        let maxAcceptJpeg
                        let left = 0.01
                        let right = quality
                        while (true) {
                            quality = (left + right) / 2
                            jpeg = canvas.toDataURL('image/jpeg', quality)
                            if (jpeg.length <= targetSize) {
                                if (!maxAcceptJpeg || maxAcceptJpeg.length < jpeg.length ) {
                                    maxAcceptJpeg = jpeg
                                }
                            }
                            if (jpeg.length === targetSize || right - left < 0.01) {
                                break
                            } else if (jpeg.length > targetSize) {
                                right = quality
                            } else {
                                left = quality
                            }
                        }
                        if (maxAcceptJpeg && jpeg.length > targetSize) {
                            jpeg = maxAcceptJpeg
                        }
                        while (quality > 0.05 && jpeg.length > targetSize) {
                            quality = quality - 0.01
                            jpeg = canvas.toDataURL('image/jpeg', quality)
                        }
                    }
                    if (jpeg.length > targetSize) {
                        throw Error("Слишком большая картинка после сжатия, выберите другую")
                    }
                    image.src = jpeg
                    this.emitValue(image.src)
                }
            } catch (e) {
                this.$msgErr(e)
            }
        },
        selectPic() {
            this.$refs.i.select()
            this.$refs.i.click()
        }
    }
}
</script>
