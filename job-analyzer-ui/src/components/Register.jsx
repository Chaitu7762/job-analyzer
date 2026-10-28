import { useState } from "react";
import "../styles/Login.css";

function Register({ onRegister, onSwitchToLogin }) {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError("");
        setLoading(true);

        try {
            const response = await fetch("http://localhost:8080/auth/register", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, password }),
            });

            if (!response.ok) {
                setError("Username already taken or invalid.");
                return;
            }

            const data = await response.json();
            localStorage.setItem("token", data.token);
            onRegister();
        } catch (err) {
            setError("Cannot reach server. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-page">
            <div className="login-card">
                <div className="login-header">
                    <div className="login-logo">JA</div>
                    <h1 className="login-title">Create Account</h1>
                    <p className="login-subtitle">
                        Sign up to start analyzing resumes against job descriptions
                    </p>
                </div>

                <form onSubmit={handleSubmit} className="login-form">
                    <div className="login-field">
                        <label className="login-label">Username</label>
                        <input
                            type="text"
                            className="login-input"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            placeholder="Choose a username"
                            required
                        />
                    </div>

                    <div className="login-field">
                        <label className="login-label">Password</label>
                        <input
                            type="password"
                            className="login-input"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="Choose a password (min 8 characters)"
                            required
                        />
                    </div>

                    {error && <div className="login-error">{error}</div>}

                    <button
                        type="submit"
                        className="login-button"
                        disabled={loading}
                    >
                        {loading ? "Creating account..." : "Sign Up"}
                    </button>
                </form>

                <p className="login-footer">
                    Already have an account?{" "}
                    <button
                        type="button"
                        className="login-link"
                        onClick={onSwitchToLogin}
                    >
                        Sign in
                    </button>
                </p>
            </div>
        </div>
    );
}

export default Register;