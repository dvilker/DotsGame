import {apiCall, ApiEnum} from "./common/api0";

/**
 * @async
 * @param { {withUserId: Number} } a
 * @returns {Promise<void>}
 */
export function acceptOffer(a) { return apiCall("acceptOffer", a, 3) }

/**
 * @async
 * @param { {text: String, to: ?(Number)[]} } a
 * @returns {Promise<void>}
 */
export function addMessage(a) { return apiCall("addMessage", a, 3) }

/**
 * @async
 * @param { {battleId: Number, ask: Boolean} } a
 * @returns {Promise<void>}
 */
export function battleDraw(a) { return apiCall("battleDraw", a, 3) }

/**
 * @async
 * @param { {battleId: Number, x: Number, y: Number} } a
 * @returns {Promise<void>}
 */
export function battleMove(a) { return apiCall("battleMove", a, 3) }

/**
 * @async
 * @param { {battleId: Number} } a
 * @returns {Promise<void>}
 */
export function battleSurrender(a) { return apiCall("battleSurrender", a, 3) }

/**
 * @async
 * @param { {m: String} } a
 * @returns {Promise<ApiDirect>}
 */
export function confirm(a) { return apiCall("confirm", a, 0) }

/**
 * @async
 * @param { {} } a
 * @returns {Promise<GUserMe>}
 */
export function getMe(a) { return apiCall("getMe", a, 3) }

/**
 * @async
 * @param { {} } a
 * @returns {Promise<(GRules)[]>}
 */
export function getRules(a) { return apiCall("getRules", a, 3) }

/**
 * @async
 * @param { {ids: (Number)[]} } a
 * @returns {Promise<(GUser)[]>}
 */
export function getUsers(a) { return apiCall("getUsers", a, 2) }

/**
 * @async
 * @param { {login: String, password: String} } a
 * @returns {Promise<SessionInfo>}
 */
export function login(a) { return apiCall("login", a, 2) }

/**
 * @async
 * @param { {} } a
 * @returns {Promise<void>}
 */
export function logout(a) { return apiCall("logout", a, 3) }

/**
 * @async
 * @param { {name: String, email: String, password1: String, password2: String} } a
 * @returns {Promise<void>}
 */
export function register(a) { return apiCall("register", a, 2) }

/**
 * @async
 * @param { {email: String, password1: String, password2: String} } a
 * @returns {Promise<void>}
 */
export function resetPassword(a) { return apiCall("resetPassword", a, 2) }

/**
 * @async
 * @param { {name: ?String, pic: ??String, rules: ?String} } a
 * @returns {Promise<void>}
 */
export function saveUser(a) { return apiCall("saveUser", a, 3) }

/**
 * @async
 * @param { {roomId: Number} } a
 * @returns {Promise<void>}
 */
export function selectRoom(a) { return apiCall("selectRoom", a, 2) }

/**
 * @async
 * @param { {} } a
 * @returns {Promise<SessionInfo>}
 */
export function session(a) { return apiCall("session", a, 2) }

/**
 * @async
 * @param { {withUserId: Number} } a
 * @returns {Promise<void>}
 */
export function toggleOffer(a) { return apiCall("toggleOffer", a, 3) }

/**
 * @async
 * @param { {number: String, code: Number} } a
 * @returns {Promise<void>}
 */
export function verifyNumber(a) { return apiCall("verifyNumber", a, 0) }

/**
 * dotsgame.enums.BattleOver
 * @enum
 */
export const BattleOver = {
    FULL_FILL: { name: "FULL_FILL", title: "Кончились ходы" },
    SURRENDER: { name: "SURRENDER", title: "Игрок сдался" },
    TIMEOUT: { name: "TIMEOUT", title: "Таймаут" },
    DRAW: { name: "DRAW", title: "Ничья" },
    GROUND: { name: "GROUND", title: "Заземление" },
    __proto__: ApiEnum,
}

/**
 * dotsgame.enums.UserLevel
 * @enum
 */
export const UserLevel = {
    N: { name: "N", abb: "Н", title: "Новичок" },
    L3: { name: "L3", abb: "3", title: "3 разряд" },
    L2: { name: "L2", abb: "2", title: "2 разряд" },
    L1: { name: "L1", abb: "1", title: "1 разряд" },
    C: { name: "C", abb: "К", title: "Кандидат" },
    M: { name: "M", abb: "М", title: "Мастер" },
    GM: { name: "GM", abb: "Г", title: "Гроссмейстер" },
    __proto__: ApiEnum,
}
