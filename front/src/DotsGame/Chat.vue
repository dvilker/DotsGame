<template>
   <div :class="[$cs.cols, $cs.rows]">
       <div :class="[$cs.rmax, $cs.rscroll]">
           <template v-if="$root.activeRoom">
               <div v-for="m of $root.activeRoom.messages" :class="[$style.Chat__msg, m.me && $style.Chat__me, m.sys && $style.Chat__sys]">
                   <span :class="$style.Chat__hdr">{{m.time.replace(/.* (\d+:\d+).*/, '$1')}}, {{m.move}}</span> <b v-if="m.from">{{m.from.title}}<template v-if="!m.me">:</template></b> {{m.text}}
               </div>
           </template>
       </div>
       <FForm :class="[$cs.rmin]" style="display: flex" :onSubmit="send">
           <FText name="text" style="flex-basis: 100%" placeholder="Чат"/>
           <FButton submit style="flex-basis: 0; margin-left: 1px; line-height: 1em">🡢</FButton>
       </FForm>
   </div>
</template>
<script>
import FForm from "../common/Forms/form/FForm";
import FButton from "../common/Forms/FButton";
import FText from "../common/Forms/FText.vue";
import {addMessage} from "api";
import User from "./User";

export default {
    components: {User, FText, FButton, FForm},

    methods: {
        async send(data) {
            await addMessage(data)
            data.text = ''
        }
    }
}
</script>

<style lang="scss" module>
.Chat__msg {

}
.Chat__hdr {
    font-size: 80%;
    color: gray;
}
.Chat__me {
    font-style: italic;
}
.Chat__sys {
    font-style: italic;
    color: gray;
}
</style>