import { Link } from 'react-router-dom';
import Card from '../components/Card';

export default function NotFound() {
  return (
    <Card title="Page not found" className="narrow">
      <p>That page does not exist.</p>
      <Link to="/" className="btn btn-primary">Back to home</Link>
    </Card>
  );
}
