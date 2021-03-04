import { createApp } from 'vue'
import globalStylesPlugin from 's/globalStyles'
import AsyncComputed from '../common/vue-async-computed'
import Autofocus from '../common/vue-autofocus'
import Forms from '../common/vue-forms'
import Dir from '../common/vue-dir'

import DotsGame from "./DotsGame";
// import Test from "./Test";

const app = createApp(DotsGame)
app.use(AsyncComputed)
app.use(globalStylesPlugin)
app.use(Autofocus)
app.use(Forms)
app.use(Dir)
app.mixin({
    methods: {
        sideClass(v) {
            return v === 0 && this.$style.side0 || v === 1 && this.$style.side1 || this.$style.sideZ
        }
    }
})
app.mount('body');
window.onerror = null;