import {errorToString} from "./api0";
import $cs from "s/commonStyles.module.scss";

export default function (Vue) {
    Vue.directive(
        'fname',
        {
            // TODO предусмотреть то, что name может меняться
            beforeMount(el, binding, vnode) {
                let cont0 = binding.value.cont
                if (cont0) {
                    let {cont, path} = cont0.getRootCont(true)
                    cont.els.push({
                        name: binding.value.name,
                        path,
                        el
                    })
                    el._ceCb = () => {
                        if (el._ceCl) {
                            delete el._ceCl
                            el.classList.remove($cs.FErroneous)
                            cont.clearErrorFor(path, binding.value.name)
                        }
                    }
                    el.addEventListener('input', el._ceCb)
                }
            },
            beforeUnmount(el, binding, vnode) {
                let cont = binding.value.cont
                if (cont) {
                    cont = cont.getRootCont()
                    let els = cont.els;
                    for (let i = els.length - 1; i >= 0; i--) {
                        if (els[i].el === el) {
                            els.splice(i, 1)
                        }
                    }
                    el.removeEventListener('input', el._ceCb)
                }
            }
        }
    )
    Vue.directive(
        'form',
        {
            mounted(formEl, binding, vnode) {
                let errorVisible = false
                formEl.addEventListener(
                    'input',
                    ev => {
                        if (errorVisible) {
                            formEl.showErrors()
                        }
                    }
                )
                formEl.showErrors = error => {
                    console.log("showErrors", error)
                    if (!error) {
                        for (let errEl of formEl.querySelectorAll('.FError')) {
                            errEl.innerText = ''
                        }
                        errorVisible = false
                        return
                    }
                    errorVisible = true
                    let errorList
                    if (Array.isArray(error)) {
                        errorList = error
                    } else if (typeof error === 'object' && error.error) {
                        if (error.error === 'errors') {
                            errorList = error.list
                        } else {
                            errorList = [{message: errorToString(error)}]
                        }
                    } else {
                        errorList = [{message: errorToString(error)}]
                    }
                    let errorSpanByName = {}
                    let elsAndErrors = formEl.querySelectorAll('input, select, textarea, .FError'), currentErrorEl,
                        lastErrorEl
                    for (let i = elsAndErrors.length - 1; i >= 0; i--) {
                        let el = elsAndErrors[i]
                        if (el.classList.contains($cs.FError)) {
                            currentErrorEl = el
                            if (!lastErrorEl) {
                                lastErrorEl = el
                            }
                            el.innerText = ''
                        } else {
                            if (el.name && !errorSpanByName.hasOwnProperty(el.name)) {
                                errorSpanByName[el.name] = currentErrorEl
                            }
                        }
                    }
                    let commonErrors = [], firstEl
                    for (let err of errorList) {
                        if (err.param) {
                            let el = formEl.elements[err.param]
                            if (el instanceof Element && el.name && errorSpanByName.hasOwnProperty(el.name)) {
                                if (!firstEl) firstEl = el
                                let errEl = errorSpanByName[el.name]
                                errEl.innerText = errEl.innerText ? errEl.innerText + '\n' + err.message : err.message
                                continue
                            }
                        }
                        commonErrors.push(err.message)
                    }
                    console.log(firstEl)
                    if (firstEl) {
                        firstEl.focus()
                    }
                    if (commonErrors.length) {
                        commonErrors = commonErrors.join('\n')
                        if (lastErrorEl) {
                            lastErrorEl.innerText = lastErrorEl.innerText ? lastErrorEl.innerText + '\n' + commonErrors : commonErrors
                        } else {
                            alert(commonErrors)
                        }
                    }
                }
            }
        }
    );
}


