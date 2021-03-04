
let timer;

export default function (Vue) {
    let max, focus;
    Vue.directive('autofocus', {
        mounted(el, binding, vnode) {
            if (binding.value === null) {
                return
            }
            // vnode.componentInstance._autoFocusLevel = binding.value || 0;
            if (timer) {
                let v = binding.value || 0;
                if (v > max) {
                    max = v;
                    focus = el;
                    focus._af_select = binding.modifiers && binding.modifiers.select;
                }
            } else {
                max = binding.value || 0;
                focus = el;
                focus._af_select = binding.modifiers && binding.modifiers.select;
                timer = setTimeout(() => {
                    focus.focus();
                    if (focus._af_select && typeof focus.select === 'function') {
                        focus.select();
                    }
                    timer = null;
                }, 1)
            }
            el.setAttribute('data-af', binding.value || 0);
        }
    });
}



export function setFocus(element) {
    if (timer) {
        clearTimeout(timer);
        timer = null
    }
    let focusCandidates = element.querySelectorAll('[data-af]');
    let max = Number.MIN_VALUE, focus;
    for(let i=0; i<focusCandidates.length; i++) {
        let candidate = focusCandidates[i];
        let focusable = candidate.focus && !candidate.disabled;
        if (focusable && candidate.isDisabled) {
            focusable = !candidate.isDisabled();
        }
        if (focusable) {
            let level = parseInt(candidate.getAttribute('data-af')) || 0;
            if (!focus || level > max) {
                focus = candidate;
                max = level
            }
        }
    }
    if (!focus) {
        focus = element.querySelector('[autofocus]:not(:disabled)');
        if (!focus) {
            focus = element.querySelector('input:not(:disabled), select:not(:disabled), textarea:not(:disabled), button:not(:disabled), a:not(:disabled)');
            if (!focus && element.focus) {
                focus = element
            }
        }
    }
    if (focus) {
        focus.focus();
    }
}


export function isFocused(element) {
    return element === document.activeElement;
}


export function hasFocus(element) {
    let to = document.activeElement;
    while (to && to !== document.documentElement) {
        if (to === element) {
            return true;
        }
        to = to.parentNode;
    }
    return false;
}