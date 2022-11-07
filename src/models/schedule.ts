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

export default class Schedule {

    constructor(
        public station1: string,
        public station2: string,
        public station1FullName: string,
        public station2FullName: string,
        public data: any
	) { }

    groups() {
        return this.data.map(group => {
            let dates = group.dates.map(d => weekdays[new Date(d[0]).getDay()]);
            if (dates.length < 2) {
                return dates[0]
            } else {
                return `${dates[0]} - ${dates[dates.length - 1]}`
            }
        })
    }

    isEmpty() {
        return this.data.length === 0
    }

    static async asyncCreate(station1:string, station2:string, paths: Paths) {
        let data = await serverFetch(`/stops?stop1=${station1}&stop2=${station2}`)
        let station1FullName = paths.getFullName(station1);
        let station2FullName = paths.getFullName(station2);
        return new Schedule(station1, station2, station1FullName, station2FullName, data)
    }

    static empty() {
        return new Schedule('','','','',[])
    }
}
