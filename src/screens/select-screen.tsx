import { NavigationContext } from "@react-navigation/native";
import React from "react";
import { MD2Colors, ActivityIndicator } from "react-native-paper";
import SelectPage from "../pages/select-page";
import { useStore } from "../stores/store";
import { observer } from 'mobx-react-lite';

const SelectScreen = observer(() => {

    const { isPathLoading } = useStore();
    const navigation = React.useContext(NavigationContext);

    return (
        isPathLoading() ? (
            <ActivityIndicator animating={true} color={MD2Colors.red800} />
        ) : (
            <SelectPage/>
        )
    )
});

export default SelectScreen;
