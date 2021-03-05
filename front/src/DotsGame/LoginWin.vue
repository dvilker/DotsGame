<template>
    <FForm :class="[$cs.cols]" :onSubmit="submit" @reset="$emit('closeResolve')" :value="formValue" style="width: 300px">
        <template v-if="div === 0">
            <div><h1>Вход</h1></div>
            <FLabel caption="Эл. почта"><FText name="login" autocomplete="email" placeholder="sasha@example.com" v-autofocus><FBtn altKey="Equal" @click="$event.setValue(v => (parseInt(v) || 0) + 1 )">+</FBtn></FText></FLabel>
            <FLabel caption="Пароль"><FText name="password" autocomplete="current-password" password /></FLabel>
            <FError common/>
            <div :class="$cs.c6">
                <a href="#" @click.prevent="div=1">Регистрация</a>
            </div>
            <div :class="$cs.c6">
                <a href="#" @click.prevent="div=2">Изменить пароль</a>
            </div>
            <template v-if="!success">
                <div :class="$cs.c8">
                    <FButton submit>Войти</FButton>
                </div>
                <div :class="$cs.c4">
                    <FButton cancel>Отмена</FButton>
                </div>
            </template>
        </template><template v-else-if="div === 1">
            <div><h1>Регистрация</h1></div>
            <template v-if="!success">
                <FLabel
                        caption="Ваше имя"
                        notes="Два слова с большой буквы через пробел русскими или английскими буквами. Если пользователь с таким же именем уже зарегистрирован, то вашему имени будет добавлено число для уникальности."
                ><FText name="name" placeholder="Иван Кузнецов" v-autofocus autocomplete="nickname"/></FLabel>
                <FLabel
                        caption="Адрес эл. почты"
                        notes="Адрес потребуется подтвердить."
                ><FText name="email" placeholder="sasha@example.com" autocomplete="email"/></FLabel>
                <FLabel caption="Пароль"><FText name="password1" placeholder="123456" autocomplete="new-password" password/></FLabel>
                <FLabel caption="Пароль ещё раз"><FText name="password2" placeholder="123456" autocomplete="new-password" password/></FLabel>
                <FError common/>
                <div :class="$cs.c8">
                    <FButton submit>Зарегистрироваться</FButton>
                </div>
                <div :class="$cs.c4">
                    <FButton cancel>Отмена</FButton>
                </div>
            </template>
            <div v-else>
                Ссылка для подтверждения регистрации отправлена на указанную почту.
                <div :class="[$cs.FNotes, $cs.p]">
                    Ссылка отправляется только если пользователь с таким адресом ещё не существует.
                </div>
            </div>
        </template><template v-else-if="div === 2">
            <div><h1>Пароль</h1></div>
            <template v-if="!success">
                <FLabel
                        caption="Эл. почта"
                        notes="Пришлем ссылку на изменение пароля."
                ><FText name="email" placeholder="sasha@example.com" autocomplete="email" v-autofocus/></FLabel>
                <FLabel caption="Новый пароль"><FText name="password1" placeholder="123456" autocomplete="new-password" password/></FLabel>
                <FLabel caption="Пароль ещё раз"><FText name="password2" placeholder="123456" autocomplete="new-password" password/></FLabel>
                <FError common/>
                <div :class="$cs.c8">
                    <FButton submit>Сменить пароль</FButton>
                </div>
                <div :class="$cs.c4">
                    <FButton cancel>Отмена</FButton>
                </div>
            </template>
            <div v-else>
                Ссылка для подтверждения изменения пароля отправлена на указанную почту.
                <div :class="[$cs.FNotes, $cs.p]">
                    Ссылка отправляется только если пользователь с таким адресом существует и активен.
                </div>
            </div>
        </template><template v-else-if="div === 10">
            <div><h1>Профиль</h1></div>
            <div>{{ $root.user.title }}</div>
            <div><a href="#" @click.prevent="div=13">Правила игры</a></div>
            <div><a href="#" @click.prevent="div=11">Изменить имя</a></div>
            <div><a href="#" @click.prevent="div=12">Изменить фото</a></div>
            <div><a href="#" @click.prevent="div=2">Изменить пароль</a></div>
            <div :class="$cs.c8"> </div>
            <div :class="$cs.c4">
            <FButton cancel v-autofocus>Закрыть</FButton>
        </div>
        </template>
        <template v-else-if="div === 11">
            <div><h1>Имя</h1></div>
            <template v-if="!success">
                <FWaiter :ac="$asyncComputed.me">
                    <FLabel
                            caption="Ваше имя"
                            notes="Два слова с большой буквы через пробел русскими или английскими буквами. Если пользователь с таким же именем уже зарегистрирован, то вашему имени будет добавлено число для уникальности."
                    ><FText name="name" placeholder="Иван Кузнецов" v-autofocus autocomplete="nickname"/></FLabel>
                    <FError common/>
                    <div :class="$cs.c8">
                        <FButton submit>Задать имя</FButton>
                    </div>
                </FWaiter>
                <div :class="$cs.c4">
                    <FButton cancel>Отмена</FButton>
                </div>
            </template>
            <div v-else>
                Анкета сохранена.
            </div>
        </template>
        <template v-else-if="div === 12">
            <div><h1>Фото</h1></div>
            <template v-if="!success">
                <FWaiter :ac="$asyncComputed.me">
                    <FLabel
                            caption="Фото"
                            notes="Выберите квадратное фото, размером не менее 48x48. Если выберите неквадратное, из него будет вырезан квадрат (круг)."
                    ><FPic name="pic" :picSize="48" :maxLength="2048" circle/></FLabel>
                    <FError common/>
                    <div :class="$cs.c8">
                        <FButton submit>Задать фото</FButton>
                    </div>
                </FWaiter>
                <div :class="$cs.c4">
                    <FButton cancel>Отмена</FButton>
                </div>
            </template>
            <div v-else>
                Анкета сохранена.
            </div>
        </template>
        <template v-else-if="div === 13">
            <div><h1>Правила игры</h1></div>
            <template v-if="!success">
                <FWaiter :ac="$asyncComputed.me">
                    <FLabel caption="Размер поля">
                        <FEnumSelect name="ruleSize" :enum="RuleSize" @item="ex.ruleSize=$event" />
                        <div v-if="ex.ruleSize" class="FNotes">
                            Ширина: {{ ex.ruleSize.width }}, высота: {{ ex.ruleSize.height }}.
                        </div>
                    </FLabel>
                    <FLabel caption="Таймер">
                        <FEnumSelect name="ruleTimer" :enum="RuleTimer" @item="ex.ruleTimer=$event" />
                        <div v-if="ex.ruleTimer" class="FNotes">
                            Всего времени: {{ formatMS(ex.ruleTimer.fullTime) }}<br>
                            Времени на ход: {{ formatMS(ex.ruleTimer.moveTime) }}<br>
                            Увеличивать время: {{ ex.ruleTimer.addUnused ? 'Да, если ход сделан быстрее чем за ' + formatMS(ex.ruleTimer.moveTime): 'Нет' }}<br>
                            По окончании времени: {{ ex.ruleTimer.randomMove ? 'Случайный ход': 'Поражение' }}
                        </div>
                    </FLabel>
                    <FLabel caption="Стартовая позиция">
                        <FEnumSelect name="ruleStart" :enum="RuleStart" />
                    </FLabel>
                    <div :class="$cs.FNotes">
                        Когда вам предложат партию, она будет по этим правилам.
                    </div>
                    <FError common/>
                    <div :class="$cs.c8">
                        <FButton submit>Сохранить</FButton>
                    </div>
                </FWaiter>
                <div :class="$cs.c4">
                    <FButton cancel>Отмена</FButton>
                </div>
            </template>
            <div v-else>
                Настройки сохранены.
            </div>
        </template>
        <template v-if="success">
            <div :class="$cs.c8"> </div>
            <div :class="$cs.c4">
                <FButton cancel @click="$emit('closeResolve')" v-autofocus>Закрыть</FButton>
            </div>
        </template>
    </FForm>
</template>

<script>
import {getMe, login, register, resetPassword, RuleSize, RuleStart, RuleTimer, saveUser} from "api";
import FForm from "../common/Forms/form/FForm";
import FLabel from "../common/Forms/FLabel";
import FText from "../common/Forms/FText";
import FError from "../common/Forms/form/FError";
import FButton from "../common/Forms/FButton";
import FBtn from "../common/Forms/FBtn";
import FPic from "../common/Forms/FPic";
import FRadio from "../common/Forms/FRadio";
import FWaiter from "../common/Forms/FWaiter";
import {formatMS} from "../common/misc";
import FEnumSelect from "../common/Forms/FEnumSelect";

export default {
    components: {FEnumSelect, FWaiter, FRadio, FPic, FBtn, FButton, FError, FText, FLabel, FForm},
    emits: ['closeResolve'],
    created() {
        Object.assign(this, {
            RuleStart,
            RuleTimer,
            RuleSize,
        })
    },
    data() {
        return {
            formValue: {},
            ex: {},
            div: this.$root.user ? 10 : 0,
            success: false
        }
    },
    mounted() {
        // apiHandlers.onDialogs({
        //     dialogs: [
        //         {type:'confirm', message: 'Как дела?'},
        //         // {type:'confirm', message: 'Что нового?'},
        //         {type:'yesno', message: 'Окружить?', default: null},
        //         {"ci":"1116is1c9dAHcl4wtcF8M3dslYdE1iO9w data:image/jpeg;base64,/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAFA3PEY8MlBGQUZaVVBfeMiCeG5uePWvuZHI////////////////////////////////////////////////////wAALCAAYAFABAREA/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/9oACAEBAAA/ALtMl+7+NHmDPIIpWPQDqajcYIHtT8IfSl2oOuKYcbsKeDSmMY4JpyHK806imvjac0xDtGdpPvSFgWyc4p7IME4xQAGQZppXDBQfcU7D92GKSIcE1JTWQN1pPLHvT6YVbcSCKQq54LCgK4GARRtbO7OTQd7cYAp6jaMV/9k","code":"CAPTCHA","type":"captcha"}
        //     ]
        // })
    },
    asyncComputed: {
        async me() {
            if (this.div === 11 || this.div === 12 || this.div === 13) {
                return await this.getMe0()
            }
        },
    },
    methods: {
        formatMS,
        async getMe0() {
            let me = await getMe()
            Object.assign(this.formValue, me)
            this.formValue.pic = me.pic && 'up/' + me.pic
            return me
        },
        async submit(data) {
            let r
            switch (this.div) {
                case 0:
                    await login(data)
                    this.$emit('closeResolve', true)
                    break;
                case 1:
                    await register(data)
                    break;
                case 2:
                    await resetPassword(data)
                    break;
                case 11: case 12: case 13:
                    await saveUser(data)
                    break
            }
            this.success = true
        }
    }
}
</script>

<style lang="scss" module>

</style>