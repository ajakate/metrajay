import { observer } from 'mobx-react-lite';
import { MD2Colors, ActivityIndicator } from "react-native-paper";

// TODO: center this on page
const LoadingWrapper = observer((props) => {

    const loadingFunc = props.loading

    return (
        loadingFunc ? (
            <ActivityIndicator animating={true} color={MD2Colors.red800} />
        ) : 
            props.children
    )
});

export default LoadingWrapper;
