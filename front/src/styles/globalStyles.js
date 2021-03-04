import cs from './commonStyles.module.scss'

const sizes = [cs.L, cs.M, cs.S];

let sizeObserver = null;
function getSizeObserver() {
    return sizeObserver || (
        sizeObserver = new window.ResizeObserver(resized => {
            for (let r of resized) {
                let target = r.target;
                let width = r.contentRect.width;
                let add = [];
                let remove = [];
                for (let sizeLetter of sizes) {
                    let size = target._sizes[sizeLetter];
                    if (width < size) {
                        if (!target.classList.contains(sizeLetter)) {
                            add.push(sizeLetter)
                        }
                    } else {
                        if (target.classList.contains(sizeLetter)) {
                            remove.push(sizeLetter)
                        }
                    }
                }
                if (remove.length) {
                    target.classList.remove(...remove);
                }
                if (add.length) {
                    target.classList.add(...add);
                }
            }
        })
    )
}

export default function (vueApp) {
    vueApp.directive('sizes', {
        bind(el, binding) {
            el._sizes = binding.value;
            getSizeObserver().observe(el);
        },
        update(el, binding) {
            el._sizes = binding.value;
        },
        unbind(el) {
            getSizeObserver().unobserve(el);
        }
    });
    vueApp.mixin({
        beforeCreate() {
            this.$cs = cs
            // debugger;
        }
    })
}