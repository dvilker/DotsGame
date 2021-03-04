export default function (Vue) {
    Vue.directive('dir-mounted', {
        mounted(el, binding, vnode) {
            let cb = binding.value
            if (typeof cb === "function") {
                cb(el,  binding, vnode)
            }
        }
    });
}
