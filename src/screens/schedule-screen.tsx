import { NavigationContext } from "@react-navigation/native";
import { observer } from 'mobx-react-lite';
import React from "react";
import { ActivityIndicator, MD2Colors } from "react-native-paper";
import LoadingWrapper from "../components/loading-wrapper";
import SchedulePage from "../pages/schedule-page";
import { useStore } from "../stores/store";

// TODO: loading wrapper? move to app.tsx?
const ScheduleScreen = observer(() => {

    const { isScheduleLoading } = useStore();
    const navigation = React.useContext(NavigationContext);

    return (
        <LoadingWrapper loading={isScheduleLoading()}>
            <SchedulePage/>
        </LoadingWrapper>
    )
});

export default ScheduleScreen;
