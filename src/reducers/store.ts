import Schedule from "../models/schedule"

const types = {
    SET_PATHS: 'SET_PATHS',
    LOAD_SCHEDULE: 'LOAD_SCHEDULE',
    SET_SCHEDULE: 'SET_SCHEDULE'
}

export const actionCreators = {
    setPaths: (paths) => ({ type: types.SET_PATHS, payload: paths }),
    addSchedule: (schedule) => ({ type: types.LOAD_SCHEDULE, payload: schedule }),
    setActiveSchedule: (key) => ({type: types.SET_SCHEDULE, payload: key})
}

export const initialState = {
    paths: undefined,
    loadingPaths: true,
    schedules: {},
    activeSchedule: undefined,
}

export function reducer(state, action) {
    switch (action.type) {
        case types.SET_PATHS:
            return { ...state, paths: action.payload, loadingPaths: false }
        case types.LOAD_SCHEDULE:
            let sschedules = { ...state.schedules }
            sschedules[action.payload.keyName()] = action.payload
            return { ...state, schedules: sschedules, activeSchedule: action.payload.keyName()}
        case types.SET_SCHEDULE:
            let schedules = { ...state.schedules }
            schedules[action.payload] = Schedule.empty()
            return { ...state, schedules: schedules, activeSchedule: action.payload}
    }
}
