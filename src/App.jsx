import { useState } from "react";
import Login from "./components/Login";
import Register from "./components/Register";
import JobAnalyzer from "./components/JobAnalyzer";

function App() {
    const [isLoggedIn, setIsLoggedIn] = useState(
        !!localStorage.getItem("token")
    );
    const [showRegister, setShowRegister] = useState(false);

    const handleLogout = () => {
        localStorage.removeItem("token");
        setIsLoggedIn(false);
        setShowRegister(false);
    };

    if (isLoggedIn) {
        return <JobAnalyzer onLogout={handleLogout} />;
    }

    return showRegister ? (
        <Register
            onRegister={() => setIsLoggedIn(true)}
            onSwitchToLogin={() => setShowRegister(false)}
        />
    ) : (
        <Login
            onLogin={() => setIsLoggedIn(true)}
            onSwitchToRegister={() => setShowRegister(true)}
        />
    );
}

export default App;