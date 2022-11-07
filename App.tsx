import { AppRegistry } from 'react-native';
import { Provider as PaperProvider } from 'react-native-paper';
import { name as appName } from './app.json';
import Metrajay from './src/app';

export default function App() {
    return (
        <PaperProvider>
            <Metrajay />
        </PaperProvider>
    );
}

AppRegistry.registerComponent(appName, () => App);
