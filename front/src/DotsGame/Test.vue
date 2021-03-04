<template>
    <div>
        <pre>{{ formVal }}</pre>
        <FForm :value="formVal" :onSubmit="submit">

            <div style="border: 1px solid black">
                <div :class="$cs.cols">
                    <div :class="$cs.c7">
                        A: <input name="a" placeholder="a">
                        <FError/>
                    </div>
                    <div :class="$cs.c5">
                        A: <input name="a" placeholder="a">
                        <FError/>
                    </div>
                    <div :class="[$cs.c6, 'FInline']">
                        A: <input name="a" placeholder="a" size="8"><FText size="8" :name="'GABBA'"/>
                        <FError/>
                    </div>
                </div>
            </div>
            B:
            <FText name="b" #="{ Input }">
                <FBtn @click="Input.setValue(v=>(parseInt(v)||0)+1)">XXX</FBtn>
            </FText>
            <FError/>


            GABBA:
            <FText :name="'GABBA'"/>
            <FError/>

            <FArray name="z">
                <div>
                    z.x: <input name="x">
                    <FError/>
                </div>
            </FArray>

            common:
            <FError common/>
            <button :disabled="$cont.isDisabled">googo</button>
            <!--            <table>
                            <tr is="label">
                                <th>Label</th>
                                <td data-error="123">
                                    !!!
                                    <input v-label name="x" placeholder="GAMMA" pattern="\d{3}" >
                                    <div class="FError" v-fname="{cont: $cont, err: true, x: (() => {debugger})()}"></div>
                                    <div>{{ $err }}</div>
            &lt;!&ndash;                        <FError/>&ndash;&gt;
                                </td>
                            </tr>
                        </table>
                        <FLabel caption="123" description="qweq" error="123123">
                            <input name="x" placeholder="GAMMA" pattern="\d{3}" >
                        </FLabel>

                        <FLabel><input name="x" placeholder="GAMMA" pattern="\d{3}" ></FLabel>



                        <input name="x" placeholder="ALPLA" :xdisabled="$cont.isDisabled" pattern="\d" @oninvalid="e => console.log('invalid', e)">
                        <FText name="uuu" placeholder="??" @click="clc"/>
                        GAMMA23
                        !{{ $value }}!GABBAHEY

                        <FArray name="a">
                            <div>
                                <input name="y" :disabled="$cont.isDisabled" >
                                <input :name="'y'+1">
                                <TestIn/>
                                <button @click.prevent="$cont.remove()">Remove</button>
                            </div>
                        </FArray>

                        <FObject name="c" >
                            <div>
                                <input name="z">
                            </div>
                        </FObject>

                        &lt;!&ndash;            <div>Before Array: {{JSON.stringify($value.a)}}</div>&ndash;&gt;
                        <button @click.prevent="$cont.add('a', {y:777})">Add</button>
                        <FObject name="d" :default="{z:'0000000'}">
                            <div>
                                <input name="z">
                            </div>
                        </FObject>-->
        </FForm>
        <!--
                <FForm :xvalue="formVal">
                    {{ $value}}
                </FForm>-->
    </div>
</template>

<script>

import FForm from "../common/Forms/form/FForm";
import FArray from "../common/Forms/form/FArray";
import FObject from "../common/Forms/form/FObject";
import TestIn from "./TestIn";
import FText from "../common/Forms/FText";
import FError from "../common/Forms/form/FError";
import {delay} from "../common/misc";
import FBtn from "../common/Forms/FBtn";

export default {
    components: {FBtn, FError, FText, TestIn, FObject, FArray, FForm},
    data() {
        return {
            formVal: {a: '123', z: [{y: 111}, {y: 222}], "c": {z: '12'}, b: 123}
        }
    },
    methods: {
        async submit(value, ev) {
            await delay(100)
            throw [
                {param: 'a', message: 'Какая-то херня с А 1'},
                {param: 'a', message: 'Какая-то херня с А 2'},
                {param: 'b', message: 'Какая-то херня с B'},
                {param: 'z.1.x', message: 'Какая-то херня с z.1.x'},
                {param: 'mmm', message: 'Какая-то херня с MMM'},
            ]
        }
    }
}
</script>

<link>


<style lang="scss" module>
//input:invalid {
//    background-color: yellow;
//}
[data-error] {
    background-color: red;

    &:after {
        content: " " attr(data-error);
        color: yellow;
    }
}
</style>