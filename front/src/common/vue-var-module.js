const {createCompilerError, createCompoundExpression, createSimpleExpression, processExpression, findDir, assert} = require("@vue/compiler-core")
const parser = require('@babel/parser');
const shared = require('@vue/shared');

function prepareTree(node, context, varNamesOut) {
    if (node.type === 1) {
        for (let i = node.props.length - 1; i >= 0 ; i--) {
            const varDir = node.props[i];
            if (varDir.type === 7 /* DIRECTIVE */ && varDir.name === 'var') {
                assert(varDir.type === 7)
                assert(varDir.exp.type === 4)
                node.props.splice(i, 1)
                let isPre = varDir.modifiers.includes('pre')
                let isMid = varDir.modifiers.includes('mid')
                let loc
                let vForDir = findDir(node, 'for')
                let hasIf = findDir(node, 'if') || findDir(node, 'else-if')

                if (vForDir && hasIf) {
                    if (isPre) {
                        loc = node.loc
                        isPre = true
                    } else if (isMid) {
                        loc = vForDir.loc
                        isPre = true
                    } else {
                        loc = vForDir.loc
                        isPre = false
                    }
                } else if (vForDir) {
                    loc = vForDir.loc
                } else {
                    loc = node.loc
                }
                if (isPre) {
                    loc._varDirsPre = loc._varDirsPre || []
                    loc._varDirsPre.push(varDir)
                } else {
                    loc._varDirs = loc._varDirs || []
                    loc._varDirs.push(varDir)
                }
                let ast = parser.parse(varDir.exp.content, {
                    plugins: [...context.expressionPlugins, ...shared.babelParserDefaultPlugins]
                }).program.body

                if (!Array.isArray(ast) || ast.length !== 1) {
                    context.onError(createCompilerError(10001, varDir.loc, null, 'Ошибка в v-var выражении: пусто или много'))
                    return false
                }
                ast = ast[0]
                if (ast.type !== "ExpressionStatement") {
                    context.onError(createCompilerError(10001, varDir.loc, null, 'Ошибка в v-var выражении: не выражение'))
                    return false
                }
                ast = ast.expression
                if (ast.type === "SequenceExpression") {
                    ast = ast.expressions
                } else if (ast.type === "AssignmentExpression") {
                    ast = [ast]
                } else {
                    context.onError(createCompilerError(10001, varDir.loc, null, 'Ошибка в v-var выражении: непонятный тип ' + ast.type))
                    return false
                }
                for (let ae of ast) {
                    if (ae.left.type !== "Identifier") {
                        // присвоение не переменной
                        continue
                    }
                    let varName = ae.left.name
                    if (!varNamesOut.includes(varName)) {
                        varNamesOut.push(varName)
                    }
                }
                // patch only-var template
                if (node.tag === 'template' && node.props.length === 0) {
                    node.tagType = 3 /* ElementTypes.TEMPLATE */
                    node.props.push({
                        type: 7,
                        name: 'if',
                        exp: createSimpleExpression('true', false, varDir.loc, false),
                        arg: undefined,
                        modifiers: [],
                        loc: varDir.loc
                    })
                }
            }
        }
    }
    if ((node.type === 0 || node.type === 1) && node.children) {
        let r = true
        for (let child of node.children) {
            r = r && prepareTree(child, context, varNamesOut)
        }
        return r
    }
    return true
}

function processTree(parentNode, node, context) {
    if (node.loc._varDirs || node.loc._varDirsPre) {
        if (node.type === 1) {
            if (node.loc._varDirs) {
                let codegenNodeItems = ['(']
                for (const varDir of node.loc._varDirs) {
                    codegenNodeItems.push(varDir.exp, ', ')
                }
                codegenNodeItems.push(node.codegenNode, ')')
                node.codegenNode = createCompoundExpression(codegenNodeItems, node.codegenNode.loc)
            }
            if (node.loc._varDirsPre) {
                context.onError(createCompilerError(10001, node.loc, null, 'Неверное место для v-var.pre. Доступно только вместе с if или for'))
            }
        } else if (node.type === 10 /* IF_BRANCH */) {
            let codePenNode = parentNode.codegenNode
            let codePenNodeBranch = 0/*consequent*/
            for (let i = 0; i < parentNode.branches.length; i++) {
                if (parentNode.branches[i] === node) {
                    break
                }
                if (parentNode.branches[i + 1].condition) {
                    codePenNodeBranch = 0/*consequent*/
                    codePenNode = codePenNode.alternate
                    if (codePenNode.type !== 19) {
                        assert(codePenNode.type === 19)
                    }
                } else {
                    codePenNodeBranch = 1/*alternate*/
                }
            }
            if (node.loc._varDirs) {
                let codegenNodeItems = ['(']
                for (const varDir of node.loc._varDirs) {
                    codegenNodeItems.push(varDir.exp, ', ')
                }
                if (codePenNodeBranch === 0/*consequent*/) {
                    codegenNodeItems.push(codePenNode.consequent, ')')
                    codePenNode.consequent = createCompoundExpression(codegenNodeItems, codePenNode.loc)
                } else {
                    codegenNodeItems.push(codePenNode.alternate, ')')
                    codePenNode.alternate = createCompoundExpression(codegenNodeItems, codePenNode.loc)
                }
            }
            if (node.loc._varDirsPre) {
                if (codePenNodeBranch === 1/*alternate*/) {
                    context.onError(createCompilerError(10001, node.loc, null, 'Неверное место для v-var.pre. Доступно только вместе с if или for'))
                } else {
                    let codegenNodeItems = ['(']
                    for (const varDir of node.loc._varDirsPre) {
                        codegenNodeItems.push(varDir.exp, ', ')
                    }
                    codegenNodeItems.push(codePenNode.test, ')')
                    codePenNode.test = createCompoundExpression(codegenNodeItems, codePenNode.loc)
                }
            }
            delete node.loc._varDirsPre
        } else if (node.type === 9 /* IF */) {
            // no op
        } else if (node.type === 11 /* FOR */) {
            assert(node.codegenNode.type === 13)
            assert(node.codegenNode.children.type === 14)
            assert(node.codegenNode.children.arguments.length === 2)
            assert(node.codegenNode.children.arguments[1].type === 18 /* JS_FUNCTION_EXPRESSION */)
            if (node.loc._varDirs) {
                let codegenNodeItems = ['(']
                for (const varDir of node.loc._varDirs) {
                    codegenNodeItems.push(varDir.exp, ', ')
                }
                codegenNodeItems.push(node.codegenNode.children.arguments[1].returns, ')')
                node.codegenNode.children.arguments[1].returns = createCompoundExpression(codegenNodeItems, node.codegenNode.children.loc)
            }
            if (node.loc._varDirsPre) {
                let codegenNodeItems = ['(']
                for (const varDir of node.loc._varDirsPre) {
                    codegenNodeItems.push(varDir.exp, ', ')
                }
                codegenNodeItems.push(node.codegenNode.children.arguments[0], ')')
                node.codegenNode.children.arguments[0] = createCompoundExpression(codegenNodeItems, node.codegenNode.children.loc)
            }
        } else {
            context.onError(createCompilerError(10001, node.loc, null, 'Дело тёмное!'))
            debugger
        }
    }
    switch (node.type) {
        case 9 /* IF */:
            for (let i = 0; i < node.branches.length; i++) {
                processTree(node, node.branches[i], context)
            }
            break
        case 10 /* IF_BRANCH */:
        case 11 /* FOR */:
        case 1 /* ELEMENT */:
        case 0 /* ROOT */:
            for (let i = 0; i < node.children.length; i++) {
                processTree(node, node.children[i], context)
            }
            break
    }
}

function currentRootNodePart(node, context) {
    if (context.parent && context.parent.type === 0) {
        context.currentRootNode = node
        return () => {
            toggleVarIdentifiers(context, node, false)
            let found = false
            for (const child of context.root.children) {
                if (child === node) {
                    found = true
                } else if (found) {
                    toggleVarIdentifiers(context, child, true)
                    break
                }
            }
        }
    }
}

function toggleVarIdentifiers(context, node, add) {
    if (node && node.xVarNames) {
        if (add) {
            for (let v of node.xVarNames) {
                context.addIdentifiers(v)
            }
        } else {
            for (let v of node.xVarNames) {
                context.removeIdentifiers(v)
            }
        }

    }
}

function rootPart(node, context) {
    if (node.type === 0 && node.children) {
        let hasVars = false
        for (const child of node.children) {
            let varNames = []
            if (prepareTree(child, context, varNames) && varNames.length) {
                child.xVarNames = varNames
                hasVars = true
            }
        }
        context.xHasVars = hasVars
        if (hasVars) {
            toggleVarIdentifiers(context, node.children[0], true)
            return () => {
                /*if (node.xVarNodes) {
                    for (const varNode of node.xVarNodes) {
                        varNode.codegenNode.tag = createCompoundExpression(
                            [
                                '(',
                                varNode.xVarDir.exp,
                                ',',
                                varNode.codegenNode.tag,
                                ')'
                            ],
                            varNode.codegenNode.loc
                        )
                    }
                }*/
                for (const oneOfRoot of node.children) {
                    if (oneOfRoot.xVarNames) {
                        processTree(node, oneOfRoot, context)
                        oneOfRoot.codegenNode = createCompoundExpression(
                            [
                                `(() => {/* vue-vars */let ${oneOfRoot.xVarNames.join(', ')}; return `,
                                oneOfRoot.codegenNode,
                                '})()'
                            ],
                            oneOfRoot.codegenNode.loc
                        )
                    }
                }
            }
        }
    }
}

function eachPart(node, context) {
    if (node.loc._varDirs) {
        for (const varDir of node.loc._varDirs) {
            if (varDir.exp.type === 4) {
                varDir.exp = processExpression(varDir.exp, context)
            }
        }
    }
    if (node.loc._varDirsPre) {
        for (const varDir of node.loc._varDirsPre) {
            if (varDir.exp.type === 4) {
                varDir.exp = processExpression(varDir.exp, context)
            }
        }
    }
}

module.exports = function (node, context) {
    if (node.type === 0 || context.xHasVars) {
        let a = currentRootNodePart(node, context)
        let b = rootPart(node, context)
        eachPart(node, context)
        if (a || b) {
            return () => {
                a && a()
                b && b()
            }
        }
    }



    if (node.type === 1 && node.xVarNames) {
        for (let v of node.xVarNames) {
            context.addIdentifiers(v)
        }
    }


    if (node.xVarNames) {
        return () => {
            for (let v of node.xVarNames) {
                context.removeIdentifiers(v)
            }
        }
    }
}

// noinspection PointlessBooleanExpressionJS
if (false) {
    // Dummy directive declaration for ide resolution
    // noinspection UnreachableCodeJS
    Vue.directive('var', {})
}