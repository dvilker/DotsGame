// apiCall, ApiEnum

export let apiHandlers = {
    onAuth: function () {
        return Promise.reject(null)
    },
    onDialogs: function (data) {
        let confirms = []
        for (let d of data.dialogs) {
            if (d.type === 'confirm') {
                confirms.push(d.message)
            }
        }
        if (window.confirm(confirms.join('\n'))) {
            for (let d of data.dialogs) {
                if (d.type === 'confirm') {
                    d.value = 1
                }
            }
        }
        return Promise.resolve(data.dialogs)
    },
    onConnectChanged: function (connected, event) {
        console.log("apiHandlers.onConnectChanged", connected, event)
    },
    onLatencyChanged: function (latency) {}
};

let wsOpen, wsRecv, wsListeners = []


/**
 * @return {Promise<WebSocket>}
 */
export function wsConnect(forceReconnect) {
    if (forceReconnect && wsOpen) {
        wsOpen._drop()
    }
    if (wsOpen) {
        return wsOpen
    }
    let ws = new WebSocket(document.location.protocol.replace(/^http/, 'ws') + "//" + document.location.host + '/a/s/')
    let pingTimer
    let promise = new Promise((resolve, reject) => {
        try {
            let waitFirst = setTimeout(() => {
                console.log("WS connect timeout")
                promise._drop()
                reject({
                    error: 'network',
                    message: 'Connection timeout'
                })
            }, 30000)
            console.log("WS connecting")
            ws.onerror = function (e) {
                waitFirst && clearTimeout(waitFirst)
                console.log("WS onerror while connect", e)
                promise._drop()
                reject(e)
            }
            ws.onclose = function (e) {
                waitFirst && clearTimeout(waitFirst)
                console.log("WS onclose while connect", e)
                promise._drop()
                reject(e)
            }
            ws.onopen = function (e) {
                let pingTime
                function ping() {
                    pingTimer = setTimeout(() => {
                        ws.send('.') // ping
                        pingTime = Date.now()
                    }, 30000)
                }
                ping()
                console.log("WS onopen", e)
                ws.onerror = function (e) {
                    waitFirst && clearTimeout(waitFirst)
                    console.log("WS onerror", e)
                    promise._drop()
                }
                ws.onclose = function (e) {
                    waitFirst && clearTimeout(waitFirst)
                    console.log("WS onclose", e)
                    promise._drop()
                }
                ws.onmessage = function (e) {
                    if (e.data === '.') {
                        apiHandlers.onLatencyChanged(Date.now() - pingTime)
                        // pong
                        ping()
                        return;
                    }
                    console.log("WS onmessage", e)
                    let isResponse = e.data.startsWith(' ')
                    let data = JSON.parse(e.data)
                    if (isResponse) {
                        if (wsRecv) {
                            let wsr = wsRecv
                            wsRecv = null
                            try {
                                wsr.resolve(data)
                            } catch (e) {
                                console.error("wsRecv.resolve error", e)
                            }
                        } else {
                            console.warn("Unexpected response", e)
                            promise._drop()
                        }
                    } else {
                        if (waitFirst) {
                            clearTimeout(waitFirst)
                            waitFirst = null
                            resolve(ws)
                            try {
                                apiHandlers.onConnectChanged(true, data)
                            } catch (e) {
                                console.error("apiHandlers.onConnectChanged(true)", e)
                            }
                            return
                        }
                        for (let wsListener of wsListeners) {
                            try {
                                wsListener(data)
                            } catch (e) {
                                console.error("wsListener error", wsListener, e)
                            }
                        }
                    }
                }
            }
        } catch (e) {
            reject(e)
        }
    })

    wsOpen = promise
    wsOpen._drop = function () {
        if (pingTimer) {
            clearTimeout(pingTimer)
        }
        if (promise === wsOpen) {
            wsOpen = null
            try {
                apiHandlers.onConnectChanged(false)
            } catch (e) {
                console.error("apiHandlers.onConnectChanged(false)", e)
            }
        }
        try {
            delete ws.onopen
            delete ws.onmessage
            delete ws.onerror
            delete ws.onclose
            ws.close()
        } catch (e) {
            console.warn("Failed to close ws connection", e)
        }
    }
    return wsOpen
}

export function addConnectionEventListener(listener) {
    wsListeners.push(listener)
}

export function removeConnectionEventListener(listener) {
    for (let i = wsListeners.length; i >=0; i--) {
        if (wsListeners[i] === listener) {
            wsListeners.splice(i, 1)
        }
    }
}


let authPromise = null;

function needAuth() {
    if (!authPromise) {
        authPromise = apiHandlers.onAuth().catch(
            reason => {
                return Promise.reject(reason ? normalizeError(reason) : {
                    error: 'local',
                    message: 'Требуется авторизация'
                });
            }
        )
    }
    return authPromise;
}


export function apiCall(methodName, payload, flags) {
    if (methodName !== 'login') {
        return wsApiCall(methodName, payload, (flags & 1) !== 0)
    }

    if (payload === undefined) {
        payload = null;
    }
    let noAuth = (flags & 1) !== 0;
    if (noAuth) {
        return callApiImmediately(methodName, payload, true)
    } else {
        if (authPromise) {
            return authPromise.then(() => callApiImmediately(methodName, payload))
        } else {
            return callApiImmediately(methodName, payload)
        }
    }
}

function setAndGetSecretCookie() {
    let r = /\bs=([a-zA-Z0-9]{6})\b/.exec(document.cookie);
    if (r) {
        return r[1]
    } else {
        let r = Math.round(Math.random() * Number.MAX_SAFE_INTEGER).toString(36);
        document.cookie = 's=' + r + '; path=/';
        return r;
    }
}

let wsCallQueue = [], wsIsCalling = null;

/**
 * @async
 */
export function wsApiCall(methodName, payload, noAuth) {
    return new Promise((resolve, reject) => {
        // Queue
        wsCallQueue.push({methodName, payload, noAuth, resolve, reject})
        wsApiCallFlush()
    })
}

async function wsApiCallFlush() {
    if (wsIsCalling) {
        return
    }
    while (wsCallQueue.length) {
        wsIsCalling = wsCallQueue.pop()
        try {
            try {
                wsIsCalling.resolve(await wsApiCallOne(wsIsCalling))
            } catch (e) {
                wsIsCalling.reject(e)
            }
        } finally {
            wsIsCalling = null
        }
    }
}

async function wsApiCallOne({methodName, payload, noAuth}) {
    let sendData = {
        _: methodName,
        ...payload,
    }
    let forceReconnect = false
    while (true) {
        let ws = await wsConnect(forceReconnect)

        ws.send(JSON.stringify(sendData))
        let result = await new Promise((resolve, reject) => wsRecv = {resolve, reject})
        // try {
        //     result = JSON.parse(result);
        // } catch (e) {
        //     throw {
        //         error: 'network',
        //         message: "Ошибка разбора ответа сервера: " + e
        //     }
        // }
        result = normalizeError(result);
        if (!result) {
            throw {
                error: 'network',
                message: "Некорректный ответ сервера"
            }
        }
        if (result.error === 'auth') {
            if (noAuth) {
                throw {error: 'local', message: 'Требуется авторизация'}
            } else {
                await needAuth();
                forceReconnect = true
                continue
            }
        }
        if (result.error === 'dialogs' && result.dialogs) {
            let dialogs = await apiHandlers.onDialogs(result);
            for (let d of dialogs) {
                if (d.value === undefined) {
                    throw {error: 'aborted'}
                }
                sendData["_$" + d.code] = d.value;
            }
            continue
        }
        if (result.error) {
            throw result;
        }
        return result.result;
    }
}


export async function callApiImmediately(methodName, payload, noAuth, dialogPayload) {
    let response;
    try {
        response = await fetch(
            '/a/' + methodName,
            {
                body: JSON.stringify(payload || {}),
                method: 'POST',
                cache: 'no-cache',
                credentials: "same-origin",
                headers: {
                    'Content-Type': 'application/json',
                    'X-S': setAndGetSecretCookie()
                },
            }
        );
    } catch (e) {
        throw {
            error: 'network',
            message: "Сетевая ошибка: " + e
        }
    }
    let data;
    if (response.status === 200) {
        try {
            data = await response.json();
        } catch (e) {
            throw {
                error: 'network',
                message: "Ошибка разбора ответа сервера: " + e
            }
        }
        data = normalizeError(data);
        if (!data) {
            throw {
                error: 'network',
                message: "Некорректный ответ сервера"
            }
        }
        if (data.error === 'dialogs' && data.dialogs) {
            let dialogs = await apiHandlers.onDialogs(data);
            dialogPayload = dialogPayload || {};
            for (let d of dialogs) {
                if (d.value === undefined) {
                    return Promise.reject({error: 'aborted'})
                }
                dialogPayload["_$" + d.code] = d.value;
            }
            return callApiImmediately(methodName, {...payload, ...dialogPayload}, noAuth, dialogPayload)
        }
        if (data.error) {
            throw data;
        }
        if (methodName === 'login') {
            await wsConnect(true)
        }
        return data.result;
    }
    if (response.status === 401) {
        if (noAuth) {
            throw {error: 'local', message: 'Требуется авторизация'}
        } else {
            await needAuth();
            return callApiImmediately(methodName, payload);
        }
    }
    console.warn('Server api error', methodName, payload, data);
    throw {
        error: 'server',
        message: "Ошибка сервера " + response.status + (data && (": " + (data.message || data)) || '')
    };
}

export function normalizeError(data) {
    if (data === null || data === undefined) {
        return null
    }
    if (data instanceof Error) {
        return {
            error: 'local',
            message: data.message,
            exception: data
        }
    }
    if (typeof data === "string" || typeof data === "object" && data instanceof String) {
        return {
            error: 'local',
            message: data
        }
    }
    if (data.error) {
        switch (data.error) {
            case 'dialogs':
            case 'local':
            case 'network':
            case 'server':
                if (!data.message && data.exception && data.exception.stack) {
                    data.message = data.exception.stack.replace(/[\n\r].*$/s, "")
                }
                break;
            case 'errors':
                if (!data.list && data.errors) {
                    data.list = [];
                    for (let k in data.errors) {
                        if (data.errors.hasOwnProperty(k)) {
                            data.list.push({
                                param: k,
                                message: data.errors[k]
                            })
                        }
                    }
                }
                if (data.errors) {
                    delete data.errors;
                }
                if (!data.list || data.list.length === 0) {
                    data.list = [{
                        param: null,
                        message: "Неизвестная ошибка"
                    }]
                }
                data.message = data.message || data.error;
                break;
            case 'aborted':
                data.message = data.message || 'Операция прервана';
                break;
        }
        data.message = data.message || data.error;
    }
    return data;
}

export function badParam(param, message) {
    return {
        error: 'errors',
        list: [{param, message}]
    }
}

export function badUnknownParam(message) {
    return {
        error: 'errors',
        list: [{message}]
    }
}

export function errorToString(error) {
    if (!error || typeof error === 'string') {
        return error;
    }
    if (error instanceof Error) {
        return error.message || ('' + error)
    }
    if (error.error === 'errors' && error.list) {
        if (error.list.length > 0) {
            let lines = [];
            for (let x of error.list) {
                lines.push(x.param ? x.param + ": " + x.message : x.message)
            }
            return lines.join('\n')
        } else {
            return "Неизвестная ошибка";
        }
    } else {
        return error.message || error.error;
    }
}


export const ApiEnum = Object.create(Object.prototype, {
    asPromise: {
        enumerable: false,
        value: function () {
            return Promise.resolve(this)
        }
    },
    title: {
        enumerable: false,
        value: function (name) {
            return this.hasOwnProperty(name) ? this[name].title : name;
        }
    },
    allKeys: {
        enumerable: false,
        value: function () {
            let all = []
            for (let i in this) {
                if (this.hasOwnProperty(i)) {
                    all.push(i)
                }
            }
            return all
        }
    },
    allValues: {
        enumerable: false,
        value: function () {
            let all = []
            for (let i in this) {
                if (this.hasOwnProperty(i)) {
                    all.push(this[i])
                }
            }
            return all
        }
    },
});