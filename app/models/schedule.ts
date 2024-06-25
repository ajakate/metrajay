import { serverFetch } from "../util/server";
import Paths from "./paths";

const weekdays = {
    0: 'Mon',
    1: 'Tue',
    2: 'Wed',
    3: 'Thurs',
    4: 'Fri',
    5: 'Sat',
    6: 'Sun'
}

const formatDate = (str) => {
    let am = "am"
    let [hour, minute, _] = str.split(":")
    let ihour = parseInt(hour)
    if (ihour > 11 && ihour < 24) {
        am = "pm"
    }

    if (ihour > 12) {
        ihour = ihour - 12
    }

    if (ihour > 12) {
        ihour = ihour - 12
    }

    return `${ihour}:${minute} ${am}`
}

export default class Schedule {

    parsedGroups: any[];

    constructor(
        public station1: string,
        public station2: string,
        public station1FullName: string,
        public station2FullName: string,
        public data: any
    ) {
        this.parsedGroups = this.data.map(group => {
            let dates = group.dates.map(d => weekdays[new Date(d[0]).getDay()]);
            let inbound = group.inbound.map(i => [formatDate(i[0]), formatDate(i[1])])
            let outbound = group.outbound.map(i => [formatDate(i[0]), formatDate(i[1])])
            if (dates.length < 2) {
                return [dates[0], inbound, outbound]
            } else {
                return [`${dates[0]}-${dates[dates.length - 1]}`, inbound, outbound]
            }
        })
    }

    groups() {
        return this.parsedGroups.map(g => { return g[0] })
    }

    keyName() {
        return [this.station1, this.station2]
    }

    // TODO: fix THIS!!!!!
    timesForGroup(groupName: string) {
        let g = this.parsedGroups.find(x => {
            return x[0] === groupName
        })

        return {
            inbound: g[1],
            outbound: g[2]
        }
    }

    static async asyncCreate(station1: string, station2: string, paths: Paths) {
        let data = await serverFetch(`/stops?stop1=${station1}&stop2=${station2}`)
        let station1FullName = paths.getFullName(station1);
        let station2FullName = paths.getFullName(station2);
        return new Schedule(station1, station2, station1FullName, station2FullName, data)
    }

    static empty() {
        return new Schedule('', '', '', '', [])
    }
}
