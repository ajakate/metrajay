import { makeAutoObservable, runInAction } from "mobx";
import React from 'react';
import Schedule from "../models/schedule";

class Store {

    paths = undefined;
    schedules = {};
    activeSchedule = '';
    schedule = undefined;

    isPathLoading = () => {
        return this.paths == undefined
    }

    setPaths = paths => {
        this.paths = paths;
    }

    isScheduleLoading = () => {
        return this.schedule == undefined
    }

    setSchedule = (stationKey) => {
        let [station1, station2] = stationKey
        Schedule.asyncCreate(station1, station2, this.paths)
        .then((schedule) => {
            runInAction(() => {
                this.schedule = schedule
              })
        })
    }

    constructor() {
        makeAutoObservable(this)
    }
}

const store = new Store();
export const StoreContext = React.createContext(store);
export const useStore = () => React.useContext(StoreContext);
