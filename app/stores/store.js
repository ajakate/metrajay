import { makeAutoObservable, runInAction } from "mobx";
import React from 'react';
import Schedule from "../models/schedule";
import AsyncStorage from '@react-native-async-storage/async-storage';

class Store {

    paths = undefined;
    schedules = {};
    activeSchedule = '';
    schedule = undefined;
    favorites = [];
    loadingFavorites = true

    isPathLoading = () => {
        return this.paths == undefined
    }

    setPaths = paths => {
        this.paths = paths;
    }

    isScheduleLoading = () => {
        return this.schedule == undefined
    }

    scheduleInFavorite = () => {
        return this.favorites.map(JSON.stringify).includes(JSON.stringify(this.schedule.keyName()))
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

    loadFavorites = () => {
        this.loadingFavorites = true
        AsyncStorage.getItem('@favorites').then(jsonVal => {
            if (jsonVal != null) {
                runInAction(() => {
                    this.favorites = JSON.parse(jsonVal)
                })
            }
            this.loadingFavorites = false
        })
    }

    // TODO: unused
    clearFavorites = () => {
        AsyncStorage.setItem('@favorites', JSON.stringify([])).then(() => {
            runInAction(() => {
                this.loadFavorites()
            })
        })
    }

    addFavorite = (key) => {
        this.loadingFavorites = true
        AsyncStorage.setItem('@favorites', JSON.stringify([...this.favorites, key])).then(() => {
            runInAction(() => {
                this.loadFavorites()
            })
        })
    }

    removeFavorite = key => {
        let newFavs = this.favorites.filter(x => { return JSON.stringify(x) !== JSON.stringify(key) })
        this.loadingFavorites = true
        AsyncStorage.setItem('@favorites', JSON.stringify(newFavs)).then(() => {
            runInAction(() => {
                this.loadFavorites()
            })
        })
    }

    addCurrentFavorite = () => {
        runInAction(() => {
            this.addFavorite(this.schedule.keyName())
        })
    }

    removeCurrentFavorite = () => {
        runInAction(() => {
            this.removeFavorite(this.schedule.keyName())
        })
    }

    constructor() {
        makeAutoObservable(this)
    }
}

const store = new Store();
export const StoreContext = React.createContext(store);
export const useStore = () => React.useContext(StoreContext);
