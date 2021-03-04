function setAsyncState(vm, stateObject, state) {
    vm.$data._asyncComputed[stateObject].state = state
    vm.$data._asyncComputed[stateObject].updating = state === 'updating'
    vm.$data._asyncComputed[stateObject].error = state === 'error'
    vm.$data._asyncComputed[stateObject].success = state === 'success'
}

function getterOnly(fn) {
    if (typeof fn === 'function') return fn;

    return fn.get;
}

function hasOwnProperty(object, property) {
    return Object.prototype.hasOwnProperty.call(object, property);
}

function isComputedLazy(item) {
    return hasOwnProperty(item, 'lazy') && item.lazy;
}

function isLazyActive(vm, key) {
    return vm[lazyActivePrefix + key];
}

let lazyActivePrefix = 'async_computed$lazy_active$',
    lazyDataPrefix = 'async_computed$lazy_data$';

function initLazy(data, key, value) {
    data[lazyActivePrefix + key] = false;
    data[lazyDataPrefix + key] = value;
}

function makeLazyComputed(key) {
    return {
        get: function get() {
            this[lazyActivePrefix + key] = true;
            return this[lazyDataPrefix + key];
        },
        set: function set(value) {
            this[lazyDataPrefix + key] = value;
        }
    };
}

function silentSetLazy(vm, key, value) {
    vm[lazyDataPrefix + key] = value;
}

function silentGetLazy(vm, key) {
    return vm[lazyDataPrefix + key];
}

let getGetterWatchedByArray = function getGetterWatchedByArray(computedAsyncProperty) {
    return function getter() {
        let _this = this;

        computedAsyncProperty.watch.forEach(function (key) {
            // Check if nested key is watched.
            let splittedByDot = key.split('.');
            if (splittedByDot.length === 1) {
                // If not, just access it.
                // eslint-disable-next-line no-unused-expressions
                _this[key];
            } else {
                // Access the nested propety.
                try {
                    let start = _this;
                    splittedByDot.forEach(function (part) {
                        start = start[part];
                    });
                } catch (error) {
                    console.error('AsyncComputed: bad path: ', key);
                    throw error;
                }
            }
        });
        return computedAsyncProperty.get.call(this);
    };
};

let getGetterWatchedByFunction = function getGetterWatchedByFunction(computedAsyncProperty) {
    return function getter() {
        computedAsyncProperty.watch.call(this);
        return computedAsyncProperty.get.call(this);
    };
};

function getWatchedGetter(computedAsyncProperty) {
    if (typeof computedAsyncProperty.watch === 'function') {
        return getGetterWatchedByFunction(computedAsyncProperty);
    } else if (Array.isArray(computedAsyncProperty.watch)) {
        computedAsyncProperty.watch.forEach(function (key) {
            if (typeof key !== 'string') {
                throw new Error('AsyncComputed: watch elemnts must be strings');
            }
        });
        return getGetterWatchedByArray(computedAsyncProperty);
    } else {
        throw Error('AsyncComputed: watch should be function or an array');
    }
}

let DidNotUpdate = typeof Symbol === 'function' ? Symbol('did-not-update') : {};

let getGetterWithShouldUpdate = function getGetterWithShouldUpdate(asyncProprety, currentGetter) {
    return function getter() {
        return asyncProprety.shouldUpdate.call(this) ? currentGetter.call(this) : DidNotUpdate;
    };
};

let shouldNotUpdate = function shouldNotUpdate(value) {
    return DidNotUpdate === value;
};

let prefix = '_async_computed$';

let AsyncComputed = {
    install: function install(Vue, pluginOptions) {
        pluginOptions = pluginOptions || {};

        Vue.config.optionMergeStrategies.asyncComputed = Vue.config.optionMergeStrategies.computed || function (toVal, fromVal) {
            return {...fromVal, ...toVal}
        }

        Vue.mixin({
            data: function data() {
                return {
                    _asyncComputed: {}
                };
            },

            computed: {
                $asyncComputed: function $asyncComputed() {
                    return this.$data._asyncComputed;
                }
            },
            beforeCreate: function beforeCreate() {
                let asyncComputed = this.$options.asyncComputed || {};

                if (!Object.keys(asyncComputed).length) return;

                for (let key in asyncComputed) {
                    let getter = getterFn(key, asyncComputed[key]);
                    this.$options.computed[prefix + key] = getter;
                }

                this.$options.data = initDataWithAsyncComputed(this.$options, pluginOptions);
            },
            created: function created() {
                for (let key in this.$options.asyncComputed || {}) {
                    let item = this.$options.asyncComputed[key],
                        value = generateDefault.call(this, item, pluginOptions);
                    if (isComputedLazy(item)) {
                        silentSetLazy(this, key, value);
                    } else {
                        this[key] = value;
                    }
                }

                for (let _key in this.$options.asyncComputed || {}) {
                    handleAsyncComputedPropetyChanges(this, _key, pluginOptions, Vue);
                }
            }
        });
    }
};

function handleAsyncComputedPropetyChanges(vm, key, pluginOptions, Vue) {
    let promiseId = 0;
    let watcher = function watcher(newPromise) {
        let thisPromise = ++promiseId;

        if (shouldNotUpdate(newPromise)) return;

        if (!newPromise || !newPromise.then) {
            newPromise = Promise.resolve(newPromise);
        }
        setAsyncState(vm, key, 'updating');

        newPromise.then(function (value) {
            if (thisPromise !== promiseId) return;
            setAsyncState(vm, key, 'success');
            vm[key] = value;
        }).catch(function (err) {
            if (thisPromise !== promiseId) return;

            setAsyncState(vm, key, 'error');
            vm.$data._asyncComputed[key].exception = err;
            if (pluginOptions.errorHandler === false) return;

            let handler = pluginOptions.errorHandler === undefined ? console.error.bind(console, 'Error evaluating async computed property:') : pluginOptions.errorHandler;

            if (pluginOptions.useRawError) {
                handler(err, vm, err.stack);
            } else {
                handler(err.stack);
            }
        });
    };
    vm.$data._asyncComputed[key] = {
        exception: null,
        update: function update() {
            if (!vm._isDestroyed) {
                watcher(getterOnly(vm.$options.asyncComputed[key]).apply(vm));
            }
        }
    };
    setAsyncState(vm, key, 'updating');
    vm.$watch(prefix + key, watcher, {immediate: true});
}

function initDataWithAsyncComputed(options, pluginOptions) {
    let optionData = options.data;
    let asyncComputed = options.asyncComputed || {};

    return function vueAsyncComputedInjectedDataFn(vm) {
        let data = (typeof optionData === 'function' ? optionData.call(this, vm) : optionData) || {};
        for (let key in asyncComputed) {
            let item = this.$options.asyncComputed[key];

            let value = generateDefault.call(this, item, pluginOptions);
            if (isComputedLazy(item)) {
                initLazy(data, key, value);
                this.$options.computed[key] = makeLazyComputed(key);
            } else {
                data[key] = value;
            }
        }
        return data;
    };
}

function getterFn(key, fn) {
    if (typeof fn === 'function') return fn;

    let getter = fn.get;

    if (hasOwnProperty(fn, 'watch')) {
        getter = getWatchedGetter(fn);
    }

    if (hasOwnProperty(fn, 'shouldUpdate')) {
        getter = getGetterWithShouldUpdate(fn, getter);
    }

    if (isComputedLazy(fn)) {
        let nonLazy = getter;
        getter = function lazyGetter() {
            if (isLazyActive(this, key)) {
                return nonLazy.call(this);
            } else {
                return silentGetLazy(this, key);
            }
        };
    }
    return getter;
}

function generateDefault(fn, pluginOptions) {
    let defaultValue = null;

    if ('default' in fn) {
        defaultValue = fn.default;
    } else if ('default' in pluginOptions) {
        defaultValue = pluginOptions.default;
    }

    if (typeof defaultValue === 'function') {
        return defaultValue.call(this);
    } else {
        return defaultValue;
    }
}

export default AsyncComputed;
