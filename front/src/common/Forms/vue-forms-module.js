const {assert} = require("@vue/compiler-core");
const {createSimpleExpression, createCompoundExpression, createStructuralDirectiveTransform, processExpression, findProp, findDir, createObjectProperty, createCompilerError, locStub} = require("@vue/compiler-core");

/**
 * Node-трансформер для vue 3, который делает следующую работу:
 * - `<FForm>` заменяется на `<FForm #="{$cont, $value}">`
 * - `<FArray name="xxx">` заменяется на `<FArray name="xxx" :value="$value['xxx']" #="{$cont, $value, $index, $parentCont}">`
 * - `<FObject name="xxx">` заменяется на `<FObject name="xxx" :value="$value['xxx']" #="{$cont, $value, $parentCont}" @setDefault="$value['xxx'] = $event">`
 * - `<FCont>` заменяется на `<FCont #="{$cont, $value}">`
 * - любой элемент внутри `FForm`, `FArray`, `FObject`, `FCont`, который содержит аттрибут `name` преобразуется так:
 * >- аттрибут `name` удаляется
 * >- добавляется директива `v-fname="{ name: xxx, cont: $cont }"`
 * >- добавляется директива `v-model="$value['xxx']"`
 * >- добавляется атрибут `:disabled="$cont.isDisabled"` или, если у элемента уже был атрибут `:disabled="???"`, то он заменяется на `:disabled="??? || $cont.isDisabled"`
 * </ul>
 *
 * Установка в webpack:
 * ```JavaScript
 * { loader: 'vue-loader', options: {compilerOptions: { nodeTransforms: [formsNodeTransform] }} }
 * ```
 */
module.exports = function (node, context) {
    if (node.type === 1 && node.tag === 'FForm') {
        if (node.props.some(x=>x.type===7 && x.name === 'slot')) {
            context.onError(createCompilerError(10001,  nameProp.loc, null, 'FForm не должен содержать явного указания слотов #=...'))
        }
        let slotProp = {
            type: 7 /*NodeTypes.DIRECTIVE*/,
            name: 'slot',
            exp: processExpression(createSimpleExpression("{$value, $cont}"), context, true),// createCompoundExpression(["{$value, $cont}"]),
            arg: undefined,
            modifiers: [''],
            loc: node.loc
        }
        node.props.push(slotProp)
        if (context.prefixIdentifiers) {
            context.addIdentifiers(slotProp.exp)
        }
        context.scopes.vSlot++
        context.scopes.f_inCont = context.scopes.f_inCont ? context.scopes.f_inCont + 1 : 1
        return () => {
            if (context.prefixIdentifiers) {
                context.removeIdentifiers(slotProp.exp);
            }
            context.scopes.vSlot--;
            context.scopes.f_inCont--;
        };
    }
    if (node.type === 1 && node.tag === 'FCont') {
        if (node.props.some(x=>x.type===7 && x.name === 'slot')) {
            context.onError(createCompilerError(10001,  nameProp.loc, null, 'FCont не должен содержать явного указания слотов #=...'))
        }
        let slotProp = {
            type: 7 /*NodeTypes.DIRECTIVE*/,
            name: 'slot',
            exp: processExpression(createSimpleExpression("{$value, $cont}"), context, true),
            arg: undefined,
            modifiers: [''],
            loc: node.loc
        }
        node.props.push(slotProp)
        if (context.prefixIdentifiers) {
            context.addIdentifiers(slotProp.exp)
        }
        context.scopes.vSlot++
        context.scopes.f_inCont = context.scopes.f_inCont ? context.scopes.f_inCont + 1 : 1
        return () => {
            if (context.prefixIdentifiers) {
                context.removeIdentifiers(slotProp.exp);
            }
            context.scopes.vSlot--;
            context.scopes.f_inCont--;
        };
    }
    if (node.type === 1 && (node.tag === 'FArray' || node.tag === 'FObject')) {
        let nameProp = findProp(node, 'name')
        if (node.props.some(x=>x.type===7 && x.name === 'slot')) {
            context.onError(createCompilerError(10001,  nameProp.loc, null, 'FArray и FObject не должены содержать явного указания слотов #=...'))
        }
        if (!nameProp) {
            context.onError(createCompilerError(10001,  nameProp.loc, null, 'FArray и FObject должены содержать свойство name'))
        }
        let nameExpr
        if (nameProp.type === 6 /*ATTRIBUTE*/) {
            nameExpr = JSON.stringify(nameProp.value.content)
        } else if (nameProp.type === 7 /*DIRECTIVE*/ && nameProp.name === 'bind') {
            nameExpr = nameProp.exp.content
        } else {
            context.onError(createCompilerError(10001,  nameProp.loc, null, 'Неизвестный тип узла имени'))
        }
        node.props.push({
            type: 7 /*NodeTypes.DIRECTIVE*/,
            name: 'bind',
            exp: `$value[${nameExpr}]`,
            arg: createSimpleExpression('value', true, locStub, true),
            modifiers: [],
            loc: node.loc
        })
        if (findProp(node, 'default')) {
            node.props.push({
                type: 7 /*NodeTypes.DIRECTIVE*/,
                name: 'on',
                exp: createSimpleExpression(`$value[${nameExpr}]=$event`),
                arg: createSimpleExpression('setDefault', true, locStub, true),
                modifiers: [],
                loc: node.loc
            })
        }
        let slotProp = {
            type: 7 /*NodeTypes.DIRECTIVE*/,
            name: 'slot',
            exp: processExpression(createSimpleExpression(node.tag === 'FArray' ? "{$parentCont, $cont, $value, $index}" : "{$parentCont, $cont, $value}"), context, true),// createCompoundExpression(["{$value, $cont}"]),
            arg: undefined,
            modifiers: [''],
            loc: node.loc
        }
        node.props.push(slotProp)
        if (context.prefixIdentifiers) {
            context.addIdentifiers(slotProp.exp)
        }
        context.scopes.vSlot++
        context.scopes.f_inCont = context.scopes.f_inCont ? context.scopes.f_inCont + 1 : 1
        return () => {
            if (context.prefixIdentifiers) {
                context.removeIdentifiers(slotProp.exp);
            }
            context.scopes.vSlot--;
            context.scopes.f_inCont--;
        };
    }
    if (node.type === 1 && context.scopes.f_inCont) {
        let nameProp = findProp(node, 'name')
        if (nameProp) {
            let isComponent = /^([A-Z]{2}|[a-z]-)/.test(node.tag)
            let nameExpr
            if (nameProp.type === 6 /*ATTRIBUTE*/) {
                nameExpr = JSON.stringify(nameProp.value.content)
            } else if (nameProp.type === 7 /*DIRECTIVE*/ && nameProp.name === 'bind') {
                nameExpr = nameProp.exp.content
            } else {
                context.onError(createCompilerError(10001,  nameProp.loc, null, 'Неизвестный тип узла имени'))
            }
            if (!isComponent) {
                node.props.push({
                    type: 7 /*NodeTypes.DIRECTIVE*/,
                    name: 'fname',
                    exp: processExpression(createSimpleExpression(`{name: ${nameExpr}, cont: $cont}`), context),
                    arg: undefined,
                    modifiers: [],
                    loc: nameProp.loc
                })
            }
            let vModelExpr = `$value[${nameExpr}]`
            node.props.push({
                type: 7 /*NodeTypes.DIRECTIVE*/,
                name: 'model',
                exp: processExpression(createSimpleExpression(vModelExpr, false, {...nameProp.loc, source: vModelExpr}), context),
                arg: undefined,
                modifiers: [],
                loc: nameProp.loc
            })
            if (!isComponent) {
                for (let i = node.props.length - 1; i >= 0; i--) {
                    if (node.props[i] === nameProp) {
                        node.props.splice(i, 1)
                    }
                }
            }
        }
        if (nameProp || node.tag === 'f-button' || node.tag === 'FButton' || node.tag === 'button' || node.tag === 'fieldset') {
            let disabledProp = findProp(node, 'disabled', false, true)
            if (disabledProp) {
                if (disabledProp.type === 7 /*DIRECTIVE*/) {
                    disabledProp.exp = createCompoundExpression(
                        [
                            '(',
                            disabledProp.exp,
                            ')||(',
                            processExpression(createSimpleExpression("$cont.isDisabled"), context),
                            ')'
                        ],
                        disabledProp.exp.loc
                    )
                }
            } else {
                node.props.push({
                    type: 7 /*NodeTypes.DIRECTIVE*/,
                    name: 'bind',
                    exp: processExpression(createSimpleExpression("$cont.isDisabled"), context),
                    arg: createSimpleExpression('disabled', true, locStub, true),
                    modifiers: [],
                    loc: node.loc
                })
            }
        }
    }
}