import {getUsers, UserLevel} from "api";
import {errorToString} from "../common/api0";
import {reactive} from "vue";

let all = new Map()

// export function getUser(userOrUserId) {
//     let was = JSON.stringify(userOrUserId)
//     let r = getUser0(userOrUserId)
//     console.error("getUser", was, " -> ", r)
//     return r
// }
export function getUser(userOrUserId) {
    let id = typeof userOrUserId === 'number' ? userOrUserId : userOrUserId.id
    if (!id) {
        console.error("Непонятный id пользователя", userOrUserId)
        throw Error("Непонятный id пользователя «" + userOrUserId + "»")
    }
    let user = all.get(id)
    if (user) {
        if (typeof userOrUserId !== 'number') {
            // Если получили объект, то обновляем наши поля, которые имеем
            Object.assign(user, userOrUserId)
        }
        return user
    } else {
        if (typeof userOrUserId !== 'number') {
            userOrUserId = reactive(userOrUserId)
            all.set(id, userOrUserId)
            return userOrUserId
        } else {
            let title = String(id);
            user = reactive({
                id,
                title: title,
                name: title,
                pic: null,
                level: UserLevel.N,
                score: 0
            })
            all.set(id, user)
            update(user)
            return user
        }
    }
}

let updateTimer = 0
let usersToUpdate = []
let inProcess = false

function update(user) {
    usersToUpdate.push(user)
    resetTimer()
}

function resetTimer() {
    if (inProcess) {
        return
    }
    if (updateTimer) {
        clearTimeout(updateTimer)
    }
    updateTimer = setTimeout(async () => {
        inProcess = true
        try {
            while (true) {
                let users = usersToUpdate.splice(0, 32)
                if (!users.length) {
                    break
                }
                let loaded
                try {
                    loaded = await getUsers({ids: users.map(u => u.id)})
                } catch (e) {
                    console.error("getUsers", users, e)
                    e = errorToString(e)
                    for (let user of users) {
                        if (user) {
                            user.title += '?'
                            user.name = user.title
                            user.error = e
                        }
                    }
                    continue
                }
                for (let loadedUser of loaded) {
                    for (let i=0; i<users.length; i++) {
                        let user = users[i]
                        if (user && user.id === loadedUser.id) {
                            Object.assign(user, loadedUser)
                            delete users[i]
                        }
                    }
                }
                for (let user of users) {
                    if (user) {
                        user.title += '?'
                        user.name += user.title
                        user.error = 'Не найден'
                    }
                }
            }
        } finally {
            inProcess = false
        }
    }, 10)
}