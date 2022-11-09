import { NavigationContext } from "@react-navigation/native";
import { observer } from 'mobx-react-lite';
import React from "react";
import { ActivityIndicator, MD2Colors } from "react-native-paper";
import SchedulePage from "../pages/schedule-page";
import { useStore } from "../stores/store";

// TODO: loading wrapper? move to app.tsx?
const ScheduleScreen = observer(() => {

    const { isScheduleLoading } = useStore();
    const navigation = React.useContext(NavigationContext);

    return (
        isScheduleLoading() ? (
            <ActivityIndicator animating={true} color={MD2Colors.red800} />
        ) : (
            <SchedulePage/>
        )
    )
});

export default ScheduleScreen;
