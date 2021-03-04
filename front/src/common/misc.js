/**
 * @async
 */
export function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms))
}


export function formatMS(seconds, allowNeg) {
    if (seconds < 0) {
        if (allowNeg) {
            return '−' + formatMS(-seconds)
        } else {
            seconds = 0
        }
    }
    let s = seconds % 60
    let m = Math.round((seconds - s) / 60)
    return s < 10 ? m + ":0" + s : m + ":" + s
}
export function formatNumber(number, withPlus) {
    if (number < 0) {
        return '−' + (-number)
    } else if (withPlus) {
        return '+' + number
    } else {
        return number
    }
}